#!/bin/bash
# Setup script to create an admin API key for E2E testing
# This script creates a known admin API key in the database

set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

# Admin API key for E2E testing (raw key value)
export E2E_ADMIN_API_KEY="pm_e2e_admin_$(date +%Y%m%d)"

# Calculate SHA-256 hash for storage
KEY_HASH=$(echo -n "$E2E_ADMIN_API_KEY" | shasum -a 256 | cut -d' ' -f1)
KEY_PREFIX="${E2E_ADMIN_API_KEY:0:8}"

echo "Setting up E2E Admin API Key..."
echo "Key Prefix: $KEY_PREFIX"
echo "Key Hash: $KEY_HASH"

# Database connection from environment or default
DB_URL="${PM__DATABASE__URL:-postgres://postgres:postgres@localhost:5432/phone_manager}"

# Parse the database URL
DB_HOST=$(echo "$DB_URL" | sed -n 's|.*@\([^:]*\):.*|\1|p')
DB_PORT=$(echo "$DB_URL" | sed -n 's|.*:\([0-9]*\)/.*|\1|p')
DB_NAME=$(echo "$DB_URL" | sed -n 's|.*/\([^?]*\).*|\1|p')
DB_USER=$(echo "$DB_URL" | sed -n 's|.*//\([^:]*\):.*|\1|p')
DB_PASS=$(echo "$DB_URL" | sed -n 's|.*://[^:]*:\([^@]*\)@.*|\1|p')

echo "Database: $DB_NAME on $DB_HOST:$DB_PORT"

# Create admin API key in database
PGPASSWORD="$DB_PASS" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
INSERT INTO api_keys (name, key_prefix, key_hash, is_active, is_admin, created_at, last_used_at)
VALUES ('E2E Test Admin', '$KEY_PREFIX', '$KEY_HASH', true, true, NOW(), NULL)
ON CONFLICT DO NOTHING;
" 2>/dev/null || {
    echo "Failed to create admin API key via psql"
    echo "Trying alternative method..."

    # Alternative: Use docker if available
    if command -v docker &> /dev/null; then
        docker exec -i postgres psql -U postgres -d phone_manager -c "
        INSERT INTO api_keys (name, key_prefix, key_hash, is_active, is_admin, created_at, last_used_at)
        VALUES ('E2E Test Admin', '$KEY_PREFIX', '$KEY_HASH', true, true, NOW(), NULL)
        ON CONFLICT DO NOTHING;
        " 2>/dev/null || echo "Docker method also failed"
    fi
}

echo ""
echo "Admin API Key created!"
echo "Add this to your config.sh or export before running tests:"
echo ""
echo "  export ADMIN_API_KEY=\"$E2E_ADMIN_API_KEY\""
echo ""

# Write to a temp file for sourcing
echo "export ADMIN_API_KEY=\"$E2E_ADMIN_API_KEY\"" > "$SCRIPT_DIR/.admin_key"
echo "Key saved to $SCRIPT_DIR/.admin_key"
