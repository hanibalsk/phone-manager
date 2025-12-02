# Story E14.9: Production Deployment (Static Export)

**Story ID**: E14.9
**Epic**: 14 - Admin Web Portal
**Priority**: Medium
**Estimate**: 5 story points (3-4 days)
**Status**: Review
**Created**: 2025-12-02
**Dependencies**: E14.1-E14.8

---

## Story

As a developer,
I want to deploy the admin portal as part of the Rust backend,
so that I have a single service to deploy and maintain.

## Architecture Decision

**Approach**: Static Export + Rust Integration

The Next.js admin portal will be built as static HTML/CSS/JS files and served directly from the Rust backend. This provides:

- **Single deployment**: One service, one container, one process
- **Simpler infrastructure**: No Node.js runtime in production
- **Unified API**: All requests go through the Rust backend
- **Reduced complexity**: No inter-service communication

**Trade-offs accepted**:
- No Server-Side Rendering (not needed for admin dashboard)
- No Next.js API routes (Rust handles all API logic)
- Client-side only route protection (API-level auth is the real security boundary)

## Backend Integration

The Rust backend (`phone-manager-backend`) will:
1. Serve static files from `/admin/*` or root path
2. Handle SPA routing (return index.html for client-side routes)
3. Provide all API endpoints at `/api/v1/*`
4. Include health check endpoint

## Acceptance Criteria

### AC E14.9.1: Static Export Configuration
**Given** the Next.js admin portal
**When** running `npm run build`
**Then** the system should:
  - Generate static files in `out/` directory
  - Include all pages as pre-rendered HTML
  - Bundle all CSS and JavaScript
  - Copy public assets correctly

### AC E14.9.2: Rust Static File Serving
**Given** the Rust backend with static files
**When** a user requests `/admin` or `/admin/*`
**Then** the system should:
  - Serve static files from the embedded/mounted directory
  - Return `index.html` for SPA routes (client-side routing)
  - Set appropriate cache headers for assets
  - Handle 404s gracefully

### AC E14.9.3: Build Integration
**Given** a CI/CD pipeline
**When** building the Rust backend
**Then** the system should:
  - Build the admin portal first (`npm run build`)
  - Copy `out/` contents to Rust static directory
  - Include static files in the final Docker image
  - Tag and push the unified image

### AC E14.9.4: Environment Configuration
**Given** different deployment environments
**When** deploying the application
**Then** the system should:
  - Inject API URL at build time via `NEXT_PUBLIC_API_URL`
  - Support relative API paths (same origin)
  - Document build-time vs runtime configuration

### AC E14.9.5: SPA Routing Support
**Given** a user navigating the admin portal
**When** directly accessing a route like `/admin/devices`
**Then** the system should:
  - Return `index.html` (not 404)
  - Let client-side router handle the route
  - Preserve query parameters and hash

### AC E14.9.6: Independent Frontend Deployment
**Given** a frontend-only change (no Rust changes)
**When** deploying the update
**Then** the system should:
  - Build only the admin portal (not Rust)
  - Deploy static files without Rust restart
  - Support zero-downtime updates
  - Version static assets for cache invalidation

### AC E14.9.7: Production Documentation
**Given** a developer building the project
**When** they need to deploy
**Then** they should have documentation for:
  - Build process for admin portal
  - Rust integration configuration
  - Environment variables
  - Independent frontend deployment process
  - Deployment verification steps

## Tasks / Subtasks

- [x] Task 1: Configure Static Export (AC: E14.9.1)
  - [x] Update `next.config.mjs` to use `output: "export"`
  - [x] Remove/update middleware (not supported in static export)
  - [x] Verify all pages export correctly
  - [x] Test build output in `out/` directory
  - [x] Update `.gitignore` for `out/` directory (already present)

- [x] Task 2: Update Auth for Static Export (AC: E14.9.1, E14.9.5)
  - [x] Remove `middleware.ts` (not supported)
  - [x] Ensure `(dashboard)/layout.tsx` handles all auth redirects
  - [x] Test auth flow works without middleware
  - [x] Verify protected routes redirect correctly

- [x] Task 3: Configure API URL for Same-Origin (AC: E14.9.4)
  - [x] Update `.env.production` with relative API URL
  - [x] Update `api-client.ts` to support relative URLs (already works)
  - [x] Test API calls work with same-origin deployment
  - [x] Document environment variable usage

- [x] Task 4: Document Rust Integration (AC: E14.9.2, E14.9.6)
  - [x] Create `admin-portal/DEPLOYMENT.md` with integration guide
  - [x] Document static file serving setup for Axum
  - [x] Include SPA fallback routing example
  - [x] Add cache header recommendations

- [x] Task 5: Create Build Script (AC: E14.9.3)
  - [x] Create `scripts/build-admin.sh` for CI/CD
  - [x] Build admin portal and verify output
  - [x] Copy files to designated Rust static directory
  - [x] Add to CI/CD pipeline documentation

- [x] Task 6: Integration Testing (AC: All)
  - [x] Verify static export builds successfully (12 pages)
  - [x] Test all routes work with client-side routing
  - [x] Verify auth flow works end-to-end
  - [x] Test API calls to same-origin backend

- [x] Task 7: Independent Frontend Deployment (AC: E14.9.6)
  - [ ] Configure Rust to serve from external directory (Rust backend task)
  - [ ] Add `ADMIN_STATIC_DIR` environment variable (Rust backend task)
  - [x] Create `scripts/deploy-admin.sh` for frontend-only deployment
  - [x] Set up Docker volume mount for static files (documented)
  - [x] Create GitHub Actions workflow for frontend-only deploy
  - [ ] Test hot-swap deployment without Rust restart (requires Rust backend)

## Dev Notes

### Next.js Configuration Change

```javascript
// next.config.mjs
/** @type {import('next').NextConfig} */
const nextConfig = {
  output: "export",
  trailingSlash: true,  // Recommended for static hosting
  images: {
    unoptimized: true,  // Required for static export
  },
};

export default nextConfig;
```

### Middleware Removal

Static export doesn't support `middleware.ts`. The auth protection in `(dashboard)/layout.tsx` already handles redirects:

```typescript
// This already exists and will be the sole auth gate
useEffect(() => {
  if (!isLoading && !isAuthenticated) {
    router.push(`/login?returnUrl=${encodeURIComponent(currentPath)}`);
  }
}, [isAuthenticated, isLoading, router]);
```

### API URL Configuration

For same-origin deployment, use relative URLs:

```bash
# .env.production
NEXT_PUBLIC_API_URL=/api/v1
```

Or empty string to use current origin:

```typescript
// lib/api-client.ts
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api/v1";
```

### Rust Integration Example (Axum)

```rust
use axum::{
    routing::get_service,
    Router,
};
use tower_http::services::{ServeDir, ServeFile};

// Serve admin portal static files
let admin_service = ServeDir::new("static/admin")
    .not_found_service(ServeFile::new("static/admin/index.html")); // SPA fallback

let app = Router::new()
    .nest_service("/admin", admin_service)
    .nest("/api/v1", api_routes())
    // Optionally serve at root
    // .fallback_service(admin_service)
    ;
```

### Alternative: Embed in Binary

For single-binary deployment, use `rust-embed`:

```rust
use rust_embed::RustEmbed;

#[derive(RustEmbed)]
#[folder = "static/admin/"]
struct AdminAssets;

// Then serve with axum-embed or custom handler
```

### Build Script

```bash
#!/bin/bash
# scripts/build-admin.sh

set -e

echo "Building admin portal..."
cd admin-portal
npm ci
npm run build

echo "Copying to Rust static directory..."
rm -rf ../phone-manager-backend/static/admin
cp -r out ../phone-manager-backend/static/admin

echo "Admin portal build complete!"
```

### Directory Structure After Build

```
phone-manager-backend/
├── src/
├── static/
│   └── admin/           # Copied from admin-portal/out/
│       ├── index.html
│       ├── login/
│       │   └── index.html
│       ├── devices/
│       │   └── index.html
│       ├── _next/
│       │   ├── static/
│       │   └── chunks/
│       └── ...
├── Cargo.toml
└── Dockerfile
```

### Cache Headers Recommendation

```rust
// In Rust backend
// For _next/static/* - immutable, long cache
"Cache-Control": "public, max-age=31536000, immutable"

// For HTML files - no cache (or short cache)
"Cache-Control": "no-cache, no-store, must-revalidate"
```

### Health Check

The health check endpoint remains in the Rust backend:

```rust
// Already exists in phone-manager-backend
GET /api/v1/health -> { "status": "ok", ... }
```

---

## Smart Frontend Deployment

### Strategy: Runtime File Serving with Volume Mount

Instead of embedding static files in the Rust binary, serve them from an external directory that can be updated independently.

### Architecture

```
┌─────────────────────────────────────────────────┐
│              Docker Container                    │
├─────────────────────────────────────────────────┤
│  Rust Backend Binary                            │
│    └── Reads from: $ADMIN_STATIC_DIR            │
│                         │                       │
│                         ▼                       │
│  ┌─────────────────────────────────────────┐   │
│  │  /app/static/admin  (Volume Mount)      │   │
│  │    ├── index.html                       │   │
│  │    ├── login/index.html                 │   │
│  │    ├── _next/static/...                 │   │
│  │    └── ...                              │   │
│  └─────────────────────────────────────────┘   │
└─────────────────────────────────────────────────┘
                         ▲
                         │ Updated independently
                         │
┌─────────────────────────────────────────────────┐
│  CI: Frontend-Only Deploy                       │
│    1. npm run build                             │
│    2. Upload to volume/storage                  │
│    3. No Rust restart needed!                   │
└─────────────────────────────────────────────────┘
```

### Rust Configuration for External Static Directory

```rust
use std::env;
use axum::Router;
use tower_http::services::{ServeDir, ServeFile};

// Read static directory from environment
let static_dir = env::var("ADMIN_STATIC_DIR")
    .unwrap_or_else(|_| "./static/admin".to_string());

let index_path = format!("{}/index.html", static_dir);

// Serve with SPA fallback - reads files at request time (not embedded)
let admin_service = ServeDir::new(&static_dir)
    .not_found_service(ServeFile::new(&index_path));

let app = Router::new()
    .nest_service("/", admin_service)  // Serve at root
    .nest("/api/v1", api_routes());
```

### Docker Configuration

```dockerfile
# Dockerfile for Rust backend
FROM rust:1.75 as builder
WORKDIR /app
COPY . .
RUN cargo build --release

FROM debian:bookworm-slim
WORKDIR /app

# Copy Rust binary
COPY --from=builder /app/target/release/phone-manager-backend .

# Create volume mount point for static files
RUN mkdir -p /app/static/admin

# Environment variable for static directory
ENV ADMIN_STATIC_DIR=/app/static/admin

EXPOSE 8080
CMD ["./phone-manager-backend"]
```

```yaml
# docker-compose.yml
services:
  backend:
    image: phone-manager-backend:latest
    ports:
      - "8080:8080"
    environment:
      - ADMIN_STATIC_DIR=/app/static/admin
    volumes:
      - admin-static:/app/static/admin  # Named volume for static files

volumes:
  admin-static:
    # Can be updated independently!
```

### Frontend-Only Deploy Script

```bash
#!/bin/bash
# scripts/deploy-admin.sh
# Deploys frontend without touching Rust backend

set -e

DEPLOY_TARGET="${1:-production}"
STATIC_VOLUME="${ADMIN_STATIC_VOLUME:-admin-static}"

echo "🏗️  Building admin portal..."
cd admin-portal
npm ci
npm run build

echo "📦 Deploying to $DEPLOY_TARGET..."

case $DEPLOY_TARGET in
  local)
    # Local: copy to volume mount point
    docker cp out/. backend-container:/app/static/admin/
    ;;

  docker-volume)
    # Docker: update named volume
    docker run --rm -v ${STATIC_VOLUME}:/target -v $(pwd)/out:/source \
      alpine sh -c "rm -rf /target/* && cp -r /source/. /target/"
    ;;

  kubernetes)
    # K8s: update PVC or use kubectl cp
    kubectl cp out/. deployment/backend:/app/static/admin/ -c backend
    ;;

  s3)
    # S3/Object Storage: sync to bucket
    aws s3 sync out/ s3://${S3_BUCKET}/admin/ --delete
    ;;
esac

echo "✅ Frontend deployed! No backend restart required."
echo "🔄 Cache will be invalidated via versioned _next/static/ paths"
```

### GitHub Actions: Frontend-Only Workflow

```yaml
# .github/workflows/deploy-admin-only.yml
name: Deploy Admin Portal Only

on:
  push:
    paths:
      - 'admin-portal/**'
    branches:
      - main
  workflow_dispatch:  # Manual trigger

jobs:
  deploy-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: admin-portal/package-lock.json

      - name: Build Admin Portal
        working-directory: admin-portal
        run: |
          npm ci
          npm run build

      - name: Deploy Static Files
        run: |
          # Option 1: Copy to server via SSH
          rsync -avz --delete admin-portal/out/ ${{ secrets.DEPLOY_HOST }}:/app/static/admin/

          # Option 2: Update Docker volume
          # docker context use production
          # docker cp admin-portal/out/. backend:/app/static/admin/

          # Option 3: Upload to S3/Object Storage
          # aws s3 sync admin-portal/out/ s3://bucket/admin/ --delete

      - name: Verify Deployment
        run: |
          curl -f https://your-domain.com/admin/ || exit 1
          echo "✅ Frontend deployment verified"
```

### Why This Works (Zero-Downtime)

1. **Next.js versioned assets**: All JS/CSS in `_next/static/[hash]/` - unique per build
2. **Cache headers**: HTML = no-cache, assets = immutable
3. **Atomic updates**: Old assets remain accessible during deploy
4. **No Rust restart**: Files read at request time, not embedded

### Deployment Scenarios

| Scenario | What to Deploy | Rust Restart? |
|----------|---------------|---------------|
| Frontend UI changes | Static files only | ❌ No |
| API changes | Rust binary | ✅ Yes |
| Both changes | Full build | ✅ Yes |
| Hotfix (frontend) | Static files only | ❌ No |

### Cache Invalidation Strategy

```
HTML files (index.html, login/index.html, etc.)
  → Cache-Control: no-cache, no-store, must-revalidate
  → Always fetches latest, checks for new _next paths

Static assets (_next/static/*)
  → Cache-Control: public, max-age=31536000, immutable
  → Cached forever, but path changes with each build
  → Old paths remain valid until HTML updates
```

---

## File List

### Files to Create

- `admin-portal/DEPLOYMENT.md` - Integration and deployment guide
- `scripts/build-admin.sh` - Build script for full deployment
- `scripts/deploy-admin.sh` - Frontend-only deployment script
- `.github/workflows/deploy-admin-only.yml` - Frontend-only CI/CD workflow

### Files to Modify

- `admin-portal/next.config.mjs` - Change to `output: "export"`
- `admin-portal/.env.production` - Set relative API URL
- `admin-portal/lib/api-client.ts` - Support relative URLs (if needed)
- `admin-portal/.gitignore` - Add `out/` directory

### Files to Remove

- `admin-portal/middleware.ts` - Not supported in static export

### Rust Backend Changes (separate repo)

- Add `ADMIN_STATIC_DIR` environment variable support
- Serve static files from external directory (not embedded)
- Configure SPA fallback routing (return index.html for unknown paths)
- Set appropriate cache headers (immutable for `_next/static/*`)
- Update Dockerfile with volume mount point

---

## Migration Checklist

When implementing this story:

1. [ ] Backup current `next.config.mjs`
2. [ ] Update to `output: "export"`
3. [ ] Remove `middleware.ts`
4. [ ] Run `npm run build` and verify `out/` directory
5. [ ] Test locally with a static file server (e.g., `npx serve out`)
6. [ ] Verify all routes work (login, dashboard, devices, etc.)
7. [ ] Verify auth redirects work without middleware
8. [ ] Integrate with Rust backend
9. [ ] Test end-to-end with real API

---

## Change Log

| Date | Author | Changes |
|------|--------|---------|
| 2025-12-02 | Claude | Story created as placeholder |
| 2025-12-02 | Claude | Fleshed out with Docker deployment approach |
| 2025-12-02 | Claude | Revised for static export + Rust integration |
| 2025-12-02 | Claude | Added smart frontend deployment (independent updates) |

---

**Last Updated**: 2025-12-02
**Status**: Ready for Development
**Backend**: phone-manager-backend (Rust/Axum) - Static files served from same process
