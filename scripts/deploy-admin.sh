#!/bin/bash
# Deploy Admin Portal (Frontend Only)
# Deploys static files without requiring Rust backend restart
#
# Usage:
#   ./deploy-admin.sh [target]
#
# Targets:
#   local       - Copy to local backend container (default)
#   volume      - Update Docker named volume
#   kubernetes  - Copy to Kubernetes pod
#   rsync       - Sync to remote server via rsync
#   s3          - Sync to S3 bucket

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ADMIN_DIR="$PROJECT_ROOT/admin-portal"
OUTPUT_DIR="$ADMIN_DIR/out"

# Default values (can be overridden via environment variables)
DEPLOY_TARGET="${1:-local}"
CONTAINER_NAME="${BACKEND_CONTAINER:-phone-manager-backend}"
VOLUME_NAME="${ADMIN_STATIC_VOLUME:-admin-static}"
REMOTE_HOST="${DEPLOY_HOST:-}"
REMOTE_PATH="${DEPLOY_PATH:-/app/static/admin}"
K8S_NAMESPACE="${K8S_NAMESPACE:-default}"
K8S_DEPLOYMENT="${K8S_DEPLOYMENT:-phone-manager-backend}"
K8S_CONTAINER="${K8S_CONTAINER:-backend}"
S3_BUCKET="${S3_BUCKET:-}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

echo_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

echo_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

show_help() {
    echo "Usage: $0 [target] [options]"
    echo ""
    echo "Targets:"
    echo "  local       Copy to local backend container (default)"
    echo "  volume      Update Docker named volume"
    echo "  kubernetes  Copy to Kubernetes pod"
    echo "  rsync       Sync to remote server"
    echo "  s3          Sync to S3 bucket"
    echo ""
    echo "Environment Variables:"
    echo "  BACKEND_CONTAINER    Container name for local deploy (default: phone-manager-backend)"
    echo "  ADMIN_STATIC_VOLUME  Volume name for volume deploy (default: admin-static)"
    echo "  DEPLOY_HOST          Remote host for rsync deploy"
    echo "  DEPLOY_PATH          Remote path (default: /app/static/admin)"
    echo "  K8S_NAMESPACE        Kubernetes namespace (default: default)"
    echo "  K8S_DEPLOYMENT       Kubernetes deployment name"
    echo "  K8S_CONTAINER        Container name in pod"
    echo "  S3_BUCKET            S3 bucket name"
    echo ""
    echo "Examples:"
    echo "  $0 local"
    echo "  DEPLOY_HOST=server.example.com $0 rsync"
    echo "  S3_BUCKET=my-bucket $0 s3"
}

# Check if build output exists
check_build() {
    if [ ! -d "$OUTPUT_DIR" ]; then
        echo_error "Build output not found at $OUTPUT_DIR"
        echo_info "Run ./scripts/build-admin.sh first"
        exit 1
    fi

    local file_count=$(find "$OUTPUT_DIR" -type f | wc -l | tr -d ' ')
    if [ "$file_count" -eq 0 ]; then
        echo_error "Build output is empty"
        exit 1
    fi

    echo_info "Found $file_count files in build output"
}

# Deploy to local container
deploy_local() {
    echo_step "Deploying to local container: $CONTAINER_NAME"

    if ! docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
        echo_error "Container '$CONTAINER_NAME' is not running"
        echo_info "Available containers:"
        docker ps --format '  {{.Names}}'
        exit 1
    fi

    # Copy files to container
    docker cp "$OUTPUT_DIR/." "$CONTAINER_NAME:/app/static/admin/"

    echo_info "Files copied to container"
}

# Deploy to Docker named volume
deploy_volume() {
    echo_step "Deploying to Docker volume: $VOLUME_NAME"

    # Create a temporary container to update the volume
    docker run --rm \
        -v "${VOLUME_NAME}:/target" \
        -v "$OUTPUT_DIR:/source:ro" \
        alpine sh -c "rm -rf /target/* && cp -r /source/. /target/"

    echo_info "Volume updated"
}

# Deploy to Kubernetes
deploy_kubernetes() {
    echo_step "Deploying to Kubernetes: $K8S_NAMESPACE/$K8S_DEPLOYMENT"

    if [ -z "$K8S_DEPLOYMENT" ]; then
        echo_error "K8S_DEPLOYMENT environment variable is required"
        exit 1
    fi

    # Get pod name
    local pod_name=$(kubectl get pods -n "$K8S_NAMESPACE" -l "app=$K8S_DEPLOYMENT" -o jsonpath='{.items[0].metadata.name}')

    if [ -z "$pod_name" ]; then
        echo_error "No pods found for deployment '$K8S_DEPLOYMENT'"
        exit 1
    fi

    echo_info "Found pod: $pod_name"

    # Copy files to pod
    kubectl cp "$OUTPUT_DIR/." "$K8S_NAMESPACE/$pod_name:/app/static/admin/" -c "$K8S_CONTAINER"

    echo_info "Files copied to pod"
}

# Deploy via rsync
deploy_rsync() {
    echo_step "Deploying via rsync to: $REMOTE_HOST:$REMOTE_PATH"

    if [ -z "$REMOTE_HOST" ]; then
        echo_error "DEPLOY_HOST environment variable is required"
        exit 1
    fi

    # Sync files to remote server
    rsync -avz --delete "$OUTPUT_DIR/" "$REMOTE_HOST:$REMOTE_PATH/"

    echo_info "Files synced to remote server"
}

# Deploy to S3
deploy_s3() {
    echo_step "Deploying to S3: s3://$S3_BUCKET/admin/"

    if [ -z "$S3_BUCKET" ]; then
        echo_error "S3_BUCKET environment variable is required"
        exit 1
    fi

    # Sync to S3 with appropriate cache headers
    # HTML files - no cache
    aws s3 sync "$OUTPUT_DIR/" "s3://$S3_BUCKET/admin/" \
        --exclude "*" \
        --include "*.html" \
        --cache-control "no-cache, no-store, must-revalidate" \
        --delete

    # Static assets - long cache
    aws s3 sync "$OUTPUT_DIR/" "s3://$S3_BUCKET/admin/" \
        --exclude "*.html" \
        --cache-control "public, max-age=31536000, immutable" \
        --delete

    echo_info "Files synced to S3"
}

# Main
case "$DEPLOY_TARGET" in
    local)
        check_build
        deploy_local
        ;;
    volume)
        check_build
        deploy_volume
        ;;
    kubernetes|k8s)
        check_build
        deploy_kubernetes
        ;;
    rsync)
        check_build
        deploy_rsync
        ;;
    s3)
        check_build
        deploy_s3
        ;;
    --help|-h)
        show_help
        exit 0
        ;;
    *)
        echo_error "Unknown target: $DEPLOY_TARGET"
        show_help
        exit 1
        ;;
esac

echo ""
echo_info "Frontend deployment complete!"
echo_info "No backend restart required - files are served dynamically"
echo ""
echo "Verification:"
echo "  - HTML files have 'no-cache' headers (always fetches latest)"
echo "  - JS/CSS in _next/static/ have versioned paths (new build = new paths)"
echo "  - Users will see new UI on next page load/refresh"
