import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

/**
 * Server-side authentication middleware for admin portal.
 *
 * This middleware runs on the Edge runtime and protects dashboard routes
 * by checking for authentication tokens before the page is rendered.
 *
 * Security: This provides server-side protection in addition to the
 * client-side auth checks in DashboardLayout.
 */
export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Public routes that don't require authentication
  const publicRoutes = [
    "/login",
    "/register",
    "/forgot-password",
    "/reset-password",
  ];

  // Check if this is a public route
  const isPublicRoute = publicRoutes.some(
    (route) => pathname === route || pathname.startsWith(`${route}/`)
  );

  // Allow public routes and static assets
  if (isPublicRoute || pathname.startsWith("/_next") || pathname.startsWith("/api")) {
    return NextResponse.next();
  }

  // Check for authentication token
  // Support both httpOnly cookies (preferred) and localStorage mode via custom header
  const accessToken = request.cookies.get("access_token")?.value;
  const authHeader = request.headers.get("authorization");

  const isAuthenticated = !!(accessToken || authHeader);

  // Redirect unauthenticated users to login
  if (!isAuthenticated) {
    const loginUrl = new URL("/login", request.url);
    // Preserve the original URL for redirect after login
    loginUrl.searchParams.set("returnUrl", pathname);
    return NextResponse.redirect(loginUrl);
  }

  return NextResponse.next();
}

export const config = {
  // Match all dashboard routes (everything except auth pages and static files)
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public files (images, etc.)
     */
    "/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)",
  ],
};
