import {
  loginSchema,
  forgotPasswordSchema,
  resetPasswordSchema,
  adminSettingsSchema,
  adminSettingsUpdateSchema,
  dailyLimitSchema,
  unlockRequestResponseSchema,
  envSchema,
  parseOrThrow,
  validate,
  getFieldErrors,
} from "../schemas";

describe("loginSchema", () => {
  it("should validate correct login credentials", () => {
    const validLogin = {
      email: "user@example.com",
      password: "password123",
    };
    expect(() => loginSchema.parse(validLogin)).not.toThrow();
  });

  it("should reject invalid email format", () => {
    const result = loginSchema.safeParse({
      email: "invalid-email",
      password: "password123",
    });
    expect(result.success).toBe(false);
  });

  it("should reject empty password", () => {
    const result = loginSchema.safeParse({
      email: "user@example.com",
      password: "",
    });
    expect(result.success).toBe(false);
  });

  it("should reject missing email", () => {
    const result = loginSchema.safeParse({
      password: "password123",
    });
    expect(result.success).toBe(false);
  });
});

describe("forgotPasswordSchema", () => {
  it("should validate correct email", () => {
    const validData = { email: "user@example.com" };
    expect(() => forgotPasswordSchema.parse(validData)).not.toThrow();
  });

  it("should reject invalid email format", () => {
    const result = forgotPasswordSchema.safeParse({
      email: "not-an-email",
    });
    expect(result.success).toBe(false);
  });
});

describe("resetPasswordSchema", () => {
  it("should validate correct reset password data", () => {
    const validData = {
      token: "some-reset-token",
      new_password: "Password123",
    };
    expect(() => resetPasswordSchema.parse(validData)).not.toThrow();
  });

  it("should reject password without uppercase", () => {
    const result = resetPasswordSchema.safeParse({
      token: "some-token",
      new_password: "password123",
    });
    expect(result.success).toBe(false);
  });

  it("should reject password without lowercase", () => {
    const result = resetPasswordSchema.safeParse({
      token: "some-token",
      new_password: "PASSWORD123",
    });
    expect(result.success).toBe(false);
  });

  it("should reject password without number", () => {
    const result = resetPasswordSchema.safeParse({
      token: "some-token",
      new_password: "PasswordOnly",
    });
    expect(result.success).toBe(false);
  });

  it("should reject password shorter than 8 characters", () => {
    const result = resetPasswordSchema.safeParse({
      token: "some-token",
      new_password: "Pass1",
    });
    expect(result.success).toBe(false);
  });

  it("should reject empty token", () => {
    const result = resetPasswordSchema.safeParse({
      token: "",
      new_password: "Password123",
    });
    expect(result.success).toBe(false);
  });
});

describe("adminSettingsSchema", () => {
  it("should validate correct settings", () => {
    const validSettings = {
      unlock_pin: "1234",
      default_daily_limit_minutes: 60,
      notifications_enabled: true,
      auto_approve_unlock_requests: false,
    };
    expect(() => adminSettingsSchema.parse(validSettings)).not.toThrow();
  });

  it("should reject PIN with less than 4 characters", () => {
    const result = adminSettingsSchema.safeParse({
      unlock_pin: "123",
      default_daily_limit_minutes: 60,
      notifications_enabled: true,
      auto_approve_unlock_requests: false,
    });
    expect(result.success).toBe(false);
  });

  it("should reject PIN with non-digit characters", () => {
    const result = adminSettingsSchema.safeParse({
      unlock_pin: "12ab",
      default_daily_limit_minutes: 60,
      notifications_enabled: true,
      auto_approve_unlock_requests: false,
    });
    expect(result.success).toBe(false);
  });

  it("should reject daily limit less than 1", () => {
    const result = adminSettingsSchema.safeParse({
      unlock_pin: "1234",
      default_daily_limit_minutes: 0,
      notifications_enabled: true,
      auto_approve_unlock_requests: false,
    });
    expect(result.success).toBe(false);
  });

  it("should reject daily limit greater than 1440", () => {
    const result = adminSettingsSchema.safeParse({
      unlock_pin: "1234",
      default_daily_limit_minutes: 1441,
      notifications_enabled: true,
      auto_approve_unlock_requests: false,
    });
    expect(result.success).toBe(false);
  });
});

describe("adminSettingsUpdateSchema", () => {
  it("should allow partial updates", () => {
    const partialUpdate = {
      unlock_pin: "5678",
    };
    expect(() => adminSettingsUpdateSchema.parse(partialUpdate)).not.toThrow();
  });

  it("should allow empty object", () => {
    expect(() => adminSettingsUpdateSchema.parse({})).not.toThrow();
  });
});

describe("dailyLimitSchema", () => {
  it("should validate correct limit", () => {
    const validLimit = {
      package_name: "com.example.app",
      app_name: "Example App",
      daily_limit_minutes: 60,
      enabled: true,
    };
    expect(() => dailyLimitSchema.parse(validLimit)).not.toThrow();
  });

  it("should reject invalid package name format", () => {
    const result = dailyLimitSchema.safeParse({
      package_name: "invalid",
      app_name: "Example App",
      daily_limit_minutes: 60,
      enabled: true,
    });
    expect(result.success).toBe(false);
  });

  it("should reject empty app name", () => {
    const result = dailyLimitSchema.safeParse({
      package_name: "com.example.app",
      app_name: "",
      daily_limit_minutes: 60,
      enabled: true,
    });
    expect(result.success).toBe(false);
  });
});

describe("unlockRequestResponseSchema", () => {
  it("should allow empty response", () => {
    expect(() => unlockRequestResponseSchema.parse({})).not.toThrow();
  });

  it("should allow undefined response", () => {
    expect(() => unlockRequestResponseSchema.parse({ response: undefined })).not.toThrow();
  });

  it("should reject response over 500 characters", () => {
    const result = unlockRequestResponseSchema.safeParse({
      response: "a".repeat(501),
    });
    expect(result.success).toBe(false);
  });
});

describe("envSchema", () => {
  it("should provide empty string as default API URL (same-origin)", () => {
    const result = envSchema.parse({});
    expect(result.NEXT_PUBLIC_API_URL).toBe("");
  });

  it("should accept valid absolute URL", () => {
    const result = envSchema.parse({
      NEXT_PUBLIC_API_URL: "https://api.example.com",
    });
    expect(result.NEXT_PUBLIC_API_URL).toBe("https://api.example.com");
  });

  it("should accept localhost URL for development", () => {
    const result = envSchema.parse({
      NEXT_PUBLIC_API_URL: "http://localhost:8080",
    });
    expect(result.NEXT_PUBLIC_API_URL).toBe("http://localhost:8080");
  });

  it("should accept empty string for same-origin deployment", () => {
    const result = envSchema.parse({
      NEXT_PUBLIC_API_URL: "",
    });
    expect(result.NEXT_PUBLIC_API_URL).toBe("");
  });
});

describe("parseOrThrow", () => {
  it("should return parsed data on success", () => {
    const result = parseOrThrow(adminSettingsUpdateSchema, { unlock_pin: "1234" });
    expect(result.unlock_pin).toBe("1234");
  });

  it("should throw with error messages on failure", () => {
    expect(() => parseOrThrow(adminSettingsSchema, { unlock_pin: "ab" })).toThrow(
      /Validation failed/
    );
  });

  it("should include custom prefix in error message", () => {
    expect(() =>
      parseOrThrow(adminSettingsSchema, { unlock_pin: "ab" }, "Settings error")
    ).toThrow(/Settings error/);
  });
});

describe("validate", () => {
  it("should return success true with data on valid input", () => {
    const result = validate(adminSettingsUpdateSchema, { unlock_pin: "1234" });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.unlock_pin).toBe("1234");
    }
  });

  it("should return success false with errors on invalid input", () => {
    const result = validate(adminSettingsSchema, { unlock_pin: "ab" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.errors.length).toBeGreaterThan(0);
    }
  });
});

describe("getFieldErrors", () => {
  it("should return empty object on valid input", () => {
    const errors = getFieldErrors(adminSettingsUpdateSchema, { unlock_pin: "1234" });
    expect(errors).toEqual({});
  });

  it("should return field-level errors", () => {
    const errors = getFieldErrors(adminSettingsSchema, {
      unlock_pin: "ab",
      default_daily_limit_minutes: 0,
      notifications_enabled: true,
      auto_approve_unlock_requests: false,
    });
    expect(errors.unlock_pin).toBeDefined();
    expect(errors.default_daily_limit_minutes).toBeDefined();
  });

  it("should only return first error per field", () => {
    const errors = getFieldErrors(adminSettingsSchema, {
      unlock_pin: "",
      default_daily_limit_minutes: 60,
      notifications_enabled: true,
      auto_approve_unlock_requests: false,
    });
    expect(typeof errors.unlock_pin).toBe("string");
  });
});
