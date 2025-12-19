"use client";

import { useEffect } from "react";
import { logger } from "@/lib/logger";

interface GlobalErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: GlobalErrorProps) {
  useEffect(() => {
    // Log error to monitoring service
    logger.error("Global application error", error, { digest: error.digest });
  }, [error]);

  return (
    <html lang="en">
      <body className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <div className="w-full max-w-md bg-white rounded-lg shadow-lg p-6">
          <div className="text-center">
            <div className="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100">
              <svg
                className="h-6 w-6 text-red-600"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                />
              </svg>
            </div>
            <h1 className="text-xl font-semibold text-gray-900 mb-2">
              Application Error
            </h1>
            <p className="text-gray-600 mb-4">
              A critical error occurred. Please refresh the page or try again later.
            </p>
            <div className="bg-gray-100 rounded-lg p-3 mb-4">
              <p className="text-sm font-mono text-gray-700 break-all">
                {error.message || "An unexpected error occurred"}
              </p>
              {error.digest && (
                <p className="mt-2 text-xs text-gray-500">
                  Error ID: {error.digest}
                </p>
              )}
            </div>
            <div className="flex gap-2">
              <a
                href="/"
                className="flex-1 inline-flex justify-center items-center px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
              >
                Go Home
              </a>
              <button
                onClick={reset}
                className="flex-1 inline-flex justify-center items-center px-4 py-2 border border-transparent rounded-md text-sm font-medium text-white bg-blue-600 hover:bg-blue-700"
              >
                Try Again
              </button>
            </div>
          </div>
        </div>
      </body>
    </html>
  );
}
