import {
  adminSettingsSchema,
  adminSettingsUpdateSchema,
  dailyLimitSchema,
  unlockRequestResponseSchema,
  envSchema,
  parseOrThrow,
  validate,
  getFieldErrors,
} from "../schemas";

describe("adminSettingsSchema", () => {
  it("should validate correct settings", () => {
    const validSettings = {
      unlockPin: "1234",
      defaultDailyLimitMinutes: 60,
      notificationsEnabled: true,
      autoApproveUnlockRequests: false,
    };
    expect(() => adminSettingsSchema.parse(validSettings)).not.toThrow();
  });

  it("should reject PIN with less than 4 characters", () => {
    const result = adminSettingsSchema.safeParse({
      unlockPin: "123",
      defaultDailyLimitMinutes: 60,
      notificationsEnabled: true,
      autoApproveUnlockRequests: false,
    });
    expect(result.success).toBe(false);
  });

  it("should reject PIN with non-digit characters", () => {
    const result = adminSettingsSchema.safeParse({
      unlockPin: "12ab",
      defaultDailyLimitMinutes: 60,
      notificationsEnabled: true,
      autoApproveUnlockRequests: false,
    });
    expect(result.success).toBe(false);
  });

  it("should reject daily limit less than 1", () => {
    const result = adminSettingsSchema.safeParse({
      unlockPin: "1234",
      defaultDailyLimitMinutes: 0,
      notificationsEnabled: true,
      autoApproveUnlockRequests: false,
    });
    expect(result.success).toBe(false);
  });

  it("should reject daily limit greater than 1440", () => {
    const result = adminSettingsSchema.safeParse({
      unlockPin: "1234",
      defaultDailyLimitMinutes: 1441,
      notificationsEnabled: true,
      autoApproveUnlockRequests: false,
    });
    expect(result.success).toBe(false);
  });
});

describe("adminSettingsUpdateSchema", () => {
  it("should allow partial updates", () => {
    const partialUpdate = {
      unlockPin: "5678",
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
      packageName: "com.example.app",
      appName: "Example App",
      dailyLimitMinutes: 60,
      enabled: true,
    };
    expect(() => dailyLimitSchema.parse(validLimit)).not.toThrow();
  });

  it("should reject invalid package name format", () => {
    const result = dailyLimitSchema.safeParse({
      packageName: "invalid",
      appName: "Example App",
      dailyLimitMinutes: 60,
      enabled: true,
    });
    expect(result.success).toBe(false);
  });

  it("should reject empty app name", () => {
    const result = dailyLimitSchema.safeParse({
      packageName: "com.example.app",
      appName: "",
      dailyLimitMinutes: 60,
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
  it("should provide default API URL", () => {
    const result = envSchema.parse({});
    expect(result.NEXT_PUBLIC_API_URL).toBe("http://localhost:3000/api");
  });

  it("should accept valid URL", () => {
    const result = envSchema.parse({
      NEXT_PUBLIC_API_URL: "https://api.example.com",
    });
    expect(result.NEXT_PUBLIC_API_URL).toBe("https://api.example.com");
  });
});

describe("parseOrThrow", () => {
  it("should return parsed data on success", () => {
    const result = parseOrThrow(adminSettingsUpdateSchema, { unlockPin: "1234" });
    expect(result.unlockPin).toBe("1234");
  });

  it("should throw with error messages on failure", () => {
    expect(() => parseOrThrow(adminSettingsSchema, { unlockPin: "ab" })).toThrow(
      /Validation failed/
    );
  });

  it("should include custom prefix in error message", () => {
    expect(() =>
      parseOrThrow(adminSettingsSchema, { unlockPin: "ab" }, "Settings error")
    ).toThrow(/Settings error/);
  });
});

describe("validate", () => {
  it("should return success true with data on valid input", () => {
    const result = validate(adminSettingsUpdateSchema, { unlockPin: "1234" });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.unlockPin).toBe("1234");
    }
  });

  it("should return success false with errors on invalid input", () => {
    const result = validate(adminSettingsSchema, { unlockPin: "ab" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.errors.length).toBeGreaterThan(0);
    }
  });
});

describe("getFieldErrors", () => {
  it("should return empty object on valid input", () => {
    const errors = getFieldErrors(adminSettingsUpdateSchema, { unlockPin: "1234" });
    expect(errors).toEqual({});
  });

  it("should return field-level errors", () => {
    const errors = getFieldErrors(adminSettingsSchema, {
      unlockPin: "ab",
      defaultDailyLimitMinutes: 0,
      notificationsEnabled: true,
      autoApproveUnlockRequests: false,
    });
    expect(errors.unlockPin).toBeDefined();
    expect(errors.defaultDailyLimitMinutes).toBeDefined();
  });

  it("should only return first error per field", () => {
    const errors = getFieldErrors(adminSettingsSchema, {
      unlockPin: "",
      defaultDailyLimitMinutes: 60,
      notificationsEnabled: true,
      autoApproveUnlockRequests: false,
    });
    expect(typeof errors.unlockPin).toBe("string");
  });
});
