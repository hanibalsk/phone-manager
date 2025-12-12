/**
 * Auth Mode Detection Utility
 *
 * Supports dual-mode authentication:
 * - 'localStorage': Tokens stored in localStorage (current, XSS-vulnerable)
 * - 'httpOnly': Tokens stored in httpOnly cookies (secure, requires backend support)
 *
 * Set NEXT_PUBLIC_AUTH_MODE=httpOnly to enable cookie-based auth.
 */

export type AuthMode = "localStorage" | "httpOnly";

let cachedMode: AuthMode | null = null;

/**
 * Get the current authentication mode.
 * Defaults to 'localStorage' for backward compatibility.
 */
export function getAuthMode(): AuthMode {
  if (cachedMode) return cachedMode;

  const envMode = process.env.NEXT_PUBLIC_AUTH_MODE;
  if (envMode === "httpOnly") {
    cachedMode = "httpOnly";
  } else {
    cachedMode = "localStorage";
  }

  return cachedMode;
}

/**
 * Check if httpOnly cookie mode is enabled.
 */
export function isHttpOnlyMode(): boolean {
  return getAuthMode() === "httpOnly";
}

/**
 * Check if localStorage mode is enabled.
 */
export function isLocalStorageMode(): boolean {
  return getAuthMode() === "localStorage";
}
