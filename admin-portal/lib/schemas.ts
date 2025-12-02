import { z } from "zod";

// Admin Settings Schema
export const adminSettingsSchema = z.object({
  unlockPin: z
    .string()
    .min(4, "PIN must be at least 4 characters")
    .max(8, "PIN must be at most 8 characters")
    .regex(/^\d+$/, "PIN must contain only digits"),
  defaultDailyLimitMinutes: z
    .number()
    .min(1, "Daily limit must be at least 1 minute")
    .max(1440, "Daily limit cannot exceed 24 hours (1440 minutes)"),
  notificationsEnabled: z.boolean(),
  autoApproveUnlockRequests: z.boolean(),
});

export type AdminSettingsInput = z.infer<typeof adminSettingsSchema>;

// Partial schema for updates (all fields optional)
export const adminSettingsUpdateSchema = adminSettingsSchema.partial();

export type AdminSettingsUpdateInput = z.infer<typeof adminSettingsUpdateSchema>;

// Daily Limit Schema
export const dailyLimitSchema = z.object({
  packageName: z
    .string()
    .min(1, "Package name is required")
    .regex(
      /^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/i,
      "Invalid package name format (e.g., com.example.app)"
    ),
  appName: z
    .string()
    .min(1, "App name is required")
    .max(100, "App name is too long"),
  dailyLimitMinutes: z
    .number()
    .min(1, "Daily limit must be at least 1 minute")
    .max(1440, "Daily limit cannot exceed 24 hours (1440 minutes)"),
  enabled: z.boolean(),
});

export type DailyLimitInput = z.infer<typeof dailyLimitSchema>;

// Unlock Request Response Schema
export const unlockRequestResponseSchema = z.object({
  response: z
    .string()
    .max(500, "Response message is too long")
    .optional(),
});

export type UnlockRequestResponseInput = z.infer<typeof unlockRequestResponseSchema>;

// Device Schema (for validation of device data from API)
export const deviceSchema = z.object({
  id: z.string().uuid(),
  name: z.string().min(1),
  androidId: z.string().min(1),
  enrolledAt: z.string().datetime(),
  lastSeen: z.string().datetime(),
  status: z.enum(["active", "inactive", "pending"]),
});

export type DeviceInput = z.infer<typeof deviceSchema>;

// Environment Variables Schema
export const envSchema = z.object({
  NEXT_PUBLIC_API_URL: z
    .string()
    .url("Invalid API URL")
    .optional()
    .default("http://localhost:3000/api"),
});

export type EnvConfig = z.infer<typeof envSchema>;

// Helper to validate and return typed data or throw
export function parseOrThrow<T>(
  schema: z.ZodSchema<T>,
  data: unknown,
  errorPrefix = "Validation failed"
): T {
  const result = schema.safeParse(data);
  if (!result.success) {
    const messages = result.error.issues.map((e) => e.message).join(", ");
    throw new Error(`${errorPrefix}: ${messages}`);
  }
  return result.data;
}

// Helper to validate and return result object
export function validate<T>(
  schema: z.ZodSchema<T>,
  data: unknown
): { success: true; data: T } | { success: false; errors: z.ZodIssue[] } {
  const result = schema.safeParse(data);
  if (result.success) {
    return { success: true, data: result.data };
  }
  return { success: false, errors: result.error.issues };
}

// Helper to get field-level errors for form display
export function getFieldErrors<T>(
  schema: z.ZodSchema<T>,
  data: unknown
): Record<string, string> {
  const result = schema.safeParse(data);
  if (result.success) {
    return {};
  }
  const errors: Record<string, string> = {};
  for (const issue of result.error.issues) {
    const path = issue.path.map(String).join(".");
    if (!errors[path]) {
      errors[path] = issue.message;
    }
  }
  return errors;
}
