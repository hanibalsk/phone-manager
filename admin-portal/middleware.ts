import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// Routes that don't require authentication
const publicRoutes = [
  "/login",
  "/forgot-password",
  "/reset-password",
];

// Check if a path starts with any of the public routes
function isPublicRoute(pathname: string): boolean {
  return publicRoutes.some((route) => pathname.startsWith(route));
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // Check for auth token in cookies or local storage isn't available in middleware
  // We use cookies for server-side auth check
  const token = request.cookies.get("auth_token")?.value;

  // Allow public routes
  if (isPublicRoute(pathname)) {
    // If user is authenticated and tries to access login, redirect to dashboard
    if (token && pathname === "/login") {
      return NextResponse.redirect(new URL("/", request.url));
    }
    return NextResponse.next();
  }

  // For protected routes, if no token, redirect to login with return URL
  // Note: Since we use localStorage for tokens (client-side),
  // the actual auth check happens in the AuthProvider on client
  // Middleware provides initial protection for server-rendered pages

  // We'll rely on client-side protection via AuthProvider
  // This middleware can be enhanced later to use httpOnly cookies
  return NextResponse.next();
}

export const config = {
  // Match all routes except static files, API routes, and Next.js internals
  matcher: [
    "/((?!api|_next/static|_next/image|favicon.ico|.*\\.).*)",
  ],
};
