import { envSchema } from "./schemas";

function validateEnv() {
  const env = {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
    NEXT_PUBLIC_AUTH_MODE: process.env.NEXT_PUBLIC_AUTH_MODE,
  };

  const result = envSchema.safeParse(env);

  if (!result.success) {
    const formattedErrors = result.error.issues
      .map((issue) => `  - ${issue.path.map(String).join(".")}: ${issue.message}`)
      .join("\n");

    console.error("Environment validation failed:\n" + formattedErrors);
    throw new Error("Invalid environment configuration");
  }

  return result.data;
}

// Validate and export environment config
export const env = validateEnv();

// Type-safe environment variable access
export type Env = typeof env;
