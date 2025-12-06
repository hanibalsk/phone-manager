#!/bin/bash
# update-version.sh - Validate VERSION file for Android app
# The actual version reading is done in build.gradle.kts
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
VERSION_FILE="$PROJECT_ROOT/VERSION"

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

echo "Android version validated:"
echo "  VERSION file:  $VERSION"
echo "  versionName:   $VERSION"
echo "  versionCode:   $VERSION_CODE"
echo ""
echo "Note: build.gradle.kts reads VERSION file directly at build time."
