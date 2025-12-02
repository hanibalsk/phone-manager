# Admin Portal Deployment Guide

This guide covers deploying the admin portal as a static export integrated with the Rust backend.

## Overview

The admin portal is built as a **static export** (HTML/CSS/JS) and served directly from the Rust backend. This provides:

- **Single deployment**: One service, one container, one process
- **No Node.js runtime**: Static files only in production
- **Unified API**: All requests through the Rust backend
- **Independent updates**: Frontend can be updated without Rust restart

## Build Process

### Prerequisites

- Node.js 18+ and npm
- Rust backend (`phone-manager-backend`) for serving

### Build Commands

```bash
# Install dependencies
npm ci

# Build static export
npm run build

# Output is in out/ directory
ls -la out/
```

### Build Output Structure

```
out/
├── index.html              # Landing page (redirects to login/dashboard)
├── login/
│   └── index.html          # Login page
├── forgot-password/
│   └── index.html
├── reset-password/
│   └── index.html
├── devices/
│   ├── index.html          # Devices list
│   └── usage/
│       └── index.html      # Device usage (query param: ?id=xxx)
├── unlock-requests/
│   └── index.html
├── limits/
│   └── index.html
├── settings/
│   └── index.html
├── 404.html                # Not found page
├── 404/
│   └── index.html
└── _next/
    └── static/             # Versioned JS/CSS bundles
        ├── chunks/
        └── css/
```

## Rust Backend Integration

### Environment Configuration

```rust
use std::env;

// Read static directory from environment (supports hot-swapping)
let static_dir = env::var("ADMIN_STATIC_DIR")
    .unwrap_or_else(|_| "./static/admin".to_string());
```

### Static File Serving (Axum)

```rust
use axum::Router;
use tower_http::services::{ServeDir, ServeFile};

// Create SPA-aware static file service
let static_dir = env::var("ADMIN_STATIC_DIR")
    .unwrap_or_else(|_| "./static/admin".to_string());
let index_path = format!("{}/index.html", static_dir);

// ServeDir with SPA fallback - returns index.html for unknown paths
let admin_service = ServeDir::new(&static_dir)
    .not_found_service(ServeFile::new(&index_path));

let app = Router::new()
    // API routes first (higher priority)
    .nest("/api/v1", api_routes())
    // Static files at root with SPA fallback
    .fallback_service(admin_service);
```

### Why SPA Fallback is Required

The admin portal uses client-side routing. When a user navigates to `/devices` and then refreshes:

1. Browser requests `/devices/index.html` from server
2. Server returns the HTML file
3. Client-side router (Next.js) handles the route

Without the fallback, direct navigation to `/devices` would return a 404 if the server doesn't have that file.

### Cache Headers

Configure cache headers for optimal performance:

```rust
use tower_http::set_header::SetResponseHeaderLayer;
use http::header::{CACHE_CONTROL, HeaderValue};

// For _next/static/* - immutable, long cache (files have hash in name)
// These files change name when content changes, so they can be cached forever
"Cache-Control": "public, max-age=31536000, immutable"

// For HTML files - no cache (must always fetch latest)
// This ensures users get new JS/CSS paths when we deploy
"Cache-Control": "no-cache, no-store, must-revalidate"
```

Recommended Axum middleware:

```rust
use axum::{
    routing::get_service,
    middleware,
};
use tower_http::services::ServeDir;

async fn add_cache_headers(
    uri: axum::http::Uri,
    mut response: axum::response::Response,
) -> axum::response::Response {
    let path = uri.path();

    let cache_value = if path.starts_with("/_next/static/") {
        "public, max-age=31536000, immutable"
    } else if path.ends_with(".html") || path == "/" {
        "no-cache, no-store, must-revalidate"
    } else {
        "public, max-age=3600"  // 1 hour for other assets
    };

    response.headers_mut().insert(
        CACHE_CONTROL,
        HeaderValue::from_static(cache_value),
    );

    response
}
```

## API Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | API base URL | Empty (same-origin) |
| `NEXT_PUBLIC_APP_NAME` | Application name | Phone Manager Admin |

### Same-Origin Deployment (Recommended)

When the frontend is served from the Rust backend, leave `NEXT_PUBLIC_API_URL` empty:

```bash
# .env.production
NEXT_PUBLIC_API_URL=
```

API calls will use relative paths:
- `fetch("/api/v1/auth/login")` → same server
- `fetch("/api/admin/devices")` → same server

### Cross-Origin Deployment

If frontend and backend are on different domains:

```bash
NEXT_PUBLIC_API_URL=https://api.example.com
```

Note: Requires CORS configuration on the backend.

## Deployment Strategies

### Strategy 1: Embedded in Docker Image

Build frontend as part of the Docker image:

```dockerfile
# Stage 1: Build frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /frontend
COPY admin-portal/package*.json ./
RUN npm ci
COPY admin-portal/ ./
RUN npm run build

# Stage 2: Build Rust backend
FROM rust:1.75 AS backend-builder
WORKDIR /app
COPY phone-manager-backend/ ./
RUN cargo build --release

# Stage 3: Runtime
FROM debian:bookworm-slim
WORKDIR /app

COPY --from=backend-builder /app/target/release/phone-manager-backend .
COPY --from=frontend-builder /frontend/out ./static/admin

ENV ADMIN_STATIC_DIR=/app/static/admin
EXPOSE 8080
CMD ["./phone-manager-backend"]
```

Pros:
- Single immutable artifact
- No external dependencies

Cons:
- Must rebuild entire image for frontend changes

### Strategy 2: Volume Mount (Recommended for Development)

Serve frontend from an external directory:

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
      - ./admin-portal/out:/app/static/admin:ro
```

Pros:
- Fast iteration during development
- Can update frontend without rebuild

Cons:
- Requires volume management

### Strategy 3: Independent Frontend Deployment (Production)

Deploy frontend files independently of the backend:

```bash
# Build frontend
npm run build --prefix admin-portal

# Deploy to volume/storage without touching backend
docker cp admin-portal/out/. backend-container:/app/static/admin/

# Or sync to shared storage
rsync -avz --delete admin-portal/out/ server:/app/static/admin/
```

Pros:
- Zero-downtime frontend updates
- No backend restart required
- Faster deployment cycles

Cons:
- More complex CI/CD setup

## Independent Frontend Deployment

### How It Works

1. **Versioned assets**: All JS/CSS in `_next/static/[hash]/` have unique names per build
2. **Atomic updates**: New files are added before old are removed
3. **No restart needed**: Rust reads files at request time (not embedded)
4. **Cache invalidation**: HTML points to new asset paths

### Deployment Script

See `scripts/deploy-admin.sh` for the deployment script.

### Deployment Matrix

| Change Type | What to Deploy | Rust Restart? |
|-------------|---------------|---------------|
| Frontend UI | Static files only | No |
| API changes | Rust binary | Yes |
| Both | Full build | Yes |
| Hotfix (UI) | Static files only | No |

## Verification Steps

After deployment, verify:

### 1. Static Files Are Served

```bash
curl -I https://your-domain.com/
# Should return 200 with HTML content-type

curl -I https://your-domain.com/login/
# Should return 200
```

### 2. SPA Routing Works

```bash
# Direct navigation to dashboard route
curl -I https://your-domain.com/devices/
# Should return 200 (not 404)
```

### 3. API Is Accessible

```bash
curl https://your-domain.com/api/v1/health
# Should return {"status":"ok",...}
```

### 4. Auth Flow Works

1. Navigate to `/devices/` without auth → redirected to `/login/`
2. Login with valid credentials → redirected to `/devices/`
3. Refresh page → stays on `/devices/`

### 5. Cache Headers Are Correct

```bash
# HTML should not be cached
curl -I https://your-domain.com/login/ | grep -i cache-control
# Cache-Control: no-cache, no-store, must-revalidate

# Static assets should be cached
curl -I https://your-domain.com/_next/static/chunks/main-xxx.js | grep -i cache-control
# Cache-Control: public, max-age=31536000, immutable
```

## Troubleshooting

### 404 on Direct Navigation

**Symptom**: Navigating directly to `/devices/` returns 404.

**Cause**: SPA fallback not configured.

**Fix**: Ensure Rust backend returns `index.html` for unknown paths:

```rust
let admin_service = ServeDir::new(&static_dir)
    .not_found_service(ServeFile::new(&index_path));
```

### API Calls Fail with CORS

**Symptom**: API calls fail with CORS error in browser console.

**Cause**: Frontend and backend on different origins without CORS.

**Fix**: Either:
1. Serve frontend from same origin (recommended)
2. Configure CORS on Rust backend

### Old Assets After Deploy

**Symptom**: Users see old UI after deployment.

**Cause**: Browser caching HTML files.

**Fix**: Ensure `Cache-Control: no-cache` for HTML files.

### Auth Redirect Loop

**Symptom**: Continuously redirected between login and dashboard.

**Cause**: Token not being sent or invalid.

**Fix**: Check:
1. `localStorage` has valid tokens
2. `Authorization` header is being sent
3. Backend accepts the token

## Related Files

- `next.config.mjs` - Static export configuration
- `lib/api-client.ts` - API client with same-origin support
- `lib/env.ts` - Environment variable validation
- `.env.production` - Production environment defaults
- `scripts/build-admin.sh` - Build script
- `scripts/deploy-admin.sh` - Deployment script

---

**Last Updated**: 2025-12-02
