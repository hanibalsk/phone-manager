/**
 * Centralized logging utility for the admin portal.
 *
 * In development: Logs to console with full details
 * In production: Can be extended to send to monitoring services (Sentry, LogRocket, etc.)
 *
 * Usage:
 *   import { logger } from "@/lib/logger";
 *   logger.error("Something went wrong", error);
 *   logger.warn("Warning message", { context: "value" });
 *   logger.info("Info message");
 *   logger.debug("Debug message");
 */

type LogLevel = "debug" | "info" | "warn" | "error";

interface LogContext {
  [key: string]: unknown;
}

interface Logger {
  debug: (message: string, context?: LogContext) => void;
  info: (message: string, context?: LogContext) => void;
  warn: (message: string, context?: LogContext) => void;
  error: (message: string, error?: Error | unknown, context?: LogContext) => void;
}

const isDevelopment = process.env.NODE_ENV === "development";

function formatMessage(level: LogLevel, message: string, context?: LogContext): string {
  const timestamp = new Date().toISOString();
  const contextStr = context ? ` ${JSON.stringify(context)}` : "";
  return `[${timestamp}] [${level.toUpperCase()}] ${message}${contextStr}`;
}

function sendToMonitoring(
  level: LogLevel,
  message: string,
  error?: Error | unknown,
  context?: LogContext
): void {
  // TODO: Integrate with monitoring service (Sentry, LogRocket, etc.)
  // Example Sentry integration:
  // if (typeof window !== "undefined" && window.Sentry) {
  //   if (error instanceof Error) {
  //     Sentry.captureException(error, { extra: { message, ...context } });
  //   } else {
  //     Sentry.captureMessage(message, { level, extra: context });
  //   }
  // }
}

export const logger: Logger = {
  debug: (message: string, context?: LogContext) => {
    if (isDevelopment) {
      // eslint-disable-next-line no-console
      console.debug(formatMessage("debug", message, context));
    }
  },

  info: (message: string, context?: LogContext) => {
    if (isDevelopment) {
      // eslint-disable-next-line no-console
      console.info(formatMessage("info", message, context));
    }
    sendToMonitoring("info", message, undefined, context);
  },

  warn: (message: string, context?: LogContext) => {
    if (isDevelopment) {
      // eslint-disable-next-line no-console
      console.warn(formatMessage("warn", message, context));
    }
    sendToMonitoring("warn", message, undefined, context);
  },

  error: (message: string, error?: Error | unknown, context?: LogContext) => {
    if (isDevelopment) {
      // eslint-disable-next-line no-console
      console.error(formatMessage("error", message, context), error);
    }
    sendToMonitoring("error", message, error, context);
  },
};

export default logger;
