#!/bin/bash
# Build Admin Portal for Production
# This script builds the Next.js admin portal as a static export
# and optionally copies it to the Rust backend static directory.

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
ADMIN_DIR="$PROJECT_ROOT/admin-portal"
OUTPUT_DIR="$ADMIN_DIR/out"
RUST_STATIC_DIR="${RUST_STATIC_DIR:-$PROJECT_ROOT/phone-manager-backend/static/admin}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Parse arguments
COPY_TO_RUST=false
CLEAN=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --copy-to-rust)
            COPY_TO_RUST=true
            shift
            ;;
        --rust-dir)
            RUST_STATIC_DIR="$2"
            shift 2
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --copy-to-rust     Copy built files to Rust static directory"
            echo "  --rust-dir <path>  Specify Rust static directory (default: ../phone-manager-backend/static/admin)"
            echo "  --clean            Clean output directories before build"
            echo "  --help             Show this help message"
            exit 0
            ;;
        *)
            echo_error "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Navigate to admin portal directory
cd "$ADMIN_DIR"

echo_info "Building Admin Portal..."
echo_info "Admin directory: $ADMIN_DIR"

# Clean if requested
if [ "$CLEAN" = true ]; then
    echo_info "Cleaning previous build..."
    rm -rf "$OUTPUT_DIR" .next
fi

# Install dependencies
echo_info "Installing dependencies..."
npm ci

# Run type checking
echo_info "Running type check..."
npm run type-check

# Run linting
echo_info "Running linter..."
npm run lint

# Run tests
echo_info "Running tests..."
npm test

# Build static export
echo_info "Building static export..."
npm run build

# Verify output
if [ ! -d "$OUTPUT_DIR" ]; then
    echo_error "Build failed - output directory not found: $OUTPUT_DIR"
    exit 1
fi

# Count generated files
HTML_COUNT=$(find "$OUTPUT_DIR" -name "*.html" | wc -l | tr -d ' ')
JS_COUNT=$(find "$OUTPUT_DIR" -name "*.js" | wc -l | tr -d ' ')

echo_info "Build successful!"
echo_info "  HTML files: $HTML_COUNT"
echo_info "  JS files: $JS_COUNT"
echo_info "  Output directory: $OUTPUT_DIR"

# Copy to Rust directory if requested
if [ "$COPY_TO_RUST" = true ]; then
    echo_info "Copying to Rust static directory..."

    # Create directory if it doesn't exist
    mkdir -p "$RUST_STATIC_DIR"

    # Remove old files and copy new
    rm -rf "$RUST_STATIC_DIR"/*
    cp -r "$OUTPUT_DIR"/* "$RUST_STATIC_DIR/"

    echo_info "Files copied to: $RUST_STATIC_DIR"
fi

echo ""
echo_info "Admin portal build complete!"
echo ""
echo "Next steps:"
if [ "$COPY_TO_RUST" = false ]; then
    echo "  - Run with --copy-to-rust to copy files to Rust backend"
fi
echo "  - Test locally: cd admin-portal && npx serve out"
echo "  - Deploy: Use scripts/deploy-admin.sh for production"
