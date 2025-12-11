#!/bin/bash
# update-version.sh - Validate VERSION file and sync across projects
# The actual version reading for Android is done in build.gradle.kts
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VERSION_FILE="$PROJECT_ROOT/VERSION"
ADMIN_PACKAGE_JSON="$PROJECT_ROOT/admin-portal/package.json"

# Read version from VERSION file
if [[ ! -f "$VERSION_FILE" ]]; then
    echo "ERROR: VERSION file not found at $VERSION_FILE"
    exit 1
fi

VERSION=$(cat "$VERSION_FILE" | tr -d '[:space:]')

# Validate semantic version format
if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "ERROR: Invalid version format '$VERSION'. Expected X.Y.Z"
    exit 1
fi

# Parse version components
IFS='.' read -r MAJOR MINOR PATCH <<< "$VERSION"

# Calculate versionCode: MAJOR*10000 + MINOR*100 + PATCH
VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))

echo "=== Version Sync ==="
echo ""
echo "Android app:"
echo "  VERSION file:  $VERSION"
echo "  versionName:   $VERSION"
echo "  versionCode:   $VERSION_CODE"
echo "  Note: build.gradle.kts reads VERSION file directly at build time."
echo ""

# Update admin-portal package.json version
if [[ -f "$ADMIN_PACKAGE_JSON" ]]; then
    # Use sed to update the version field in package.json
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS sed requires empty string for -i
        sed -i '' "s/\"version\": \"[^\"]*\"/\"version\": \"$VERSION\"/" "$ADMIN_PACKAGE_JSON"
    else
        # Linux sed
        sed -i "s/\"version\": \"[^\"]*\"/\"version\": \"$VERSION\"/" "$ADMIN_PACKAGE_JSON"
    fi
    echo "Admin Portal:"
    echo "  package.json:  $VERSION (updated)"
else
    echo "WARNING: Admin portal package.json not found at $ADMIN_PACKAGE_JSON"
fi

echo ""
echo "Version sync complete."
