# Phone Manager Admin Portal

A comprehensive web-based administration interface for the Phone Manager platform built with Next.js 14, React 18, and TypeScript.

## Overview

The Admin Portal enables platform administrators to manage:
- **Users & Organizations**: Multi-tenant user management with RBAC
- **Device Fleet**: Bulk device operations, enrollment tokens, inactive device management
- **Locations & Geofences**: Geographic monitoring and boundary management
- **Webhooks & Trips**: Integration configuration and trip history
- **App Usage & Limits**: Usage analytics and app restriction policies
- **System Configuration**: Auth settings, feature flags, rate limits, API keys
- **Audit & Compliance**: Activity logs, GDPR tools, integrity verification

## Technology Stack

| Category | Technology |
|----------|------------|
| Framework | Next.js 14 (App Router, Static Export) |
| Language | TypeScript 5.x |
| UI Library | React 18 |
| Components | shadcn/ui + Radix UI |
| Styling | Tailwind CSS |
| Forms | React Hook Form + Zod |
| State | React Context |
| HTTP Client | Fetch API |

## Quick Start

### Prerequisites

- Node.js 18+
- npm or yarn
- Backend API server running

### Installation

```bash
# Install dependencies
npm install

# Create environment file
cp .env.example .env.local

# Start development server
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NEXT_PUBLIC_API_URL` | Backend API base URL | `""` (same-origin) |
| `NEXT_PUBLIC_AUTH_MODE` | Authentication mode | `"localStorage"` |

## Authentication

The admin portal supports two authentication modes for flexibility and security:

### localStorage Mode (Default)

Tokens are stored in browser localStorage. This is the current default for backward compatibility.

```bash
# .env.local
NEXT_PUBLIC_AUTH_MODE=localStorage
```

**Characteristics:**
- Tokens accessible to JavaScript
- Works without backend changes
- Suitable for development and internal tools

### httpOnly Cookie Mode (Recommended for Production)

Tokens are stored in httpOnly cookies, providing XSS protection.

```bash
# .env.local
NEXT_PUBLIC_AUTH_MODE=httpOnly
```

**Characteristics:**
- Tokens inaccessible to JavaScript (XSS protection)
- Requires backend changes (see below)
- Recommended for production deployments

**Backend Requirements:**

Before enabling httpOnly mode, your backend must:
1. Set tokens as httpOnly cookies on login/register
2. Read tokens from cookies instead of Authorization header
3. Enable CORS with `Access-Control-Allow-Credentials: true`

See [Backend httpOnly Auth Requirements](../docs/backend-httponly-auth-requirements.md) for detailed implementation guide.

## Project Structure

```
admin-portal/
├── app/                    # Next.js App Router pages
│   ├── (auth)/            # Public auth pages (login, register, etc.)
│   ├── (dashboard)/       # Protected dashboard pages
│   └── layout.tsx         # Root layout
├── components/            # React components
│   ├── ui/               # shadcn/ui components
│   └── [feature]/        # Feature-specific components
├── contexts/             # React context providers
│   └── auth-context.tsx  # Authentication state
├── lib/                  # Utilities and configuration
│   ├── api-client.ts     # API request helpers
│   ├── auth-mode.ts      # Auth mode detection
│   ├── constants.ts      # App constants
│   ├── env.ts           # Environment validation
│   ├── schemas.ts       # Zod validation schemas
│   └── utils.ts         # Utility functions
├── types/               # TypeScript type definitions
└── public/              # Static assets
```

## Available Scripts

```bash
# Development
npm run dev          # Start dev server
npm run lint         # Run ESLint
npm run type-check   # TypeScript type checking

# Production
npm run build        # Build for production (static export)
npm run start        # Start production server

# Testing
npm run test         # Run tests
npm run test:watch   # Run tests in watch mode
```

## Security Features

- **RBAC**: Role-based access control with granular permissions
- **Route Protection**: Client-side route guards for protected pages
- **XSS Protection**: Optional httpOnly cookie authentication
- **Input Validation**: Zod schemas for all form inputs
- **API Security**: Bearer token or cookie-based authentication

## Deployment

The admin portal is configured for static export (`output: "export"` in next.config.mjs).

```bash
# Build static files
npm run build

# Output in: out/
```

Deploy the `out/` directory to any static hosting service (Vercel, Netlify, S3, etc.).

**Note:** Static export means no server-side features (API routes, middleware). All authentication and authorization is handled client-side with backend validation.

## Documentation

| Document | Description |
|----------|-------------|
| [PRD](../docs/PRD-admin-portal.md) | Product requirements |
| [API Spec](../docs/ADMIN_API_SPEC.md) | Admin API documentation |
| [Backend Auth](../docs/backend-httponly-auth-requirements.md) | httpOnly cookie setup |
| [Backlog](../docs/backlog.md) | Development backlog |

## Contributing

1. Follow existing code patterns and TypeScript conventions
2. Use shadcn/ui components for UI consistency
3. Add Zod schemas for form validation
4. Write tests for new features
5. Run `npm run lint` before committing
