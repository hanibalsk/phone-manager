"use client";

import { Suspense, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/contexts/auth-context";
import { registerSchema, getFieldErrors } from "@/lib/schemas";
import type { RegisterInput } from "@/lib/schemas";
import { configApi } from "@/lib/api-client";

function RegisterForm() {
  const router = useRouter();
  const { register } = useAuth();

  const [formData, setFormData] = useState<RegisterInput>({
    display_name: "",
    email: "",
    password: "",
    confirm_password: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [registerError, setRegisterError] = useState<string | null>(null);
  const [registrationEnabled, setRegistrationEnabled] = useState<boolean | null>(null);

  // Check if registration is enabled
  useEffect(() => {
    const checkConfig = async () => {
      const response = await configApi.getPublic();
      if (response.data) {
        setRegistrationEnabled(response.data.auth.registration_enabled);
        if (!response.data.auth.registration_enabled) {
          // Redirect to login if registration is disabled
          router.push("/login");
        }
      } else {
        // Default to allowing registration if config fails to load
        setRegistrationEnabled(true);
      }
    };
    checkConfig();
  }, [router]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    // Clear field error when user starts typing
    if (errors[name]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
    // Clear register error
    if (registerError) {
      setRegisterError(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setRegisterError(null);

    // Validate form
    const fieldErrors = getFieldErrors(registerSchema, formData);
    if (Object.keys(fieldErrors).length > 0) {
      setErrors(fieldErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await register({
        email: formData.email,
        password: formData.password,
        display_name: formData.display_name,
      });
      if (result.success) {
        router.push("/");
      } else {
        setRegisterError(result.error || "Registration failed");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Show loading while checking config
  if (registrationEnabled === null) {
    return (
      <div className="space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold">Create Account</h1>
          <p className="text-muted-foreground mt-2">
            Checking registration availability...
          </p>
        </div>
        <div className="flex items-center justify-center py-8">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      </div>
    );
  }

  // Don't render if registration is disabled (will redirect)
  if (!registrationEnabled) {
    return null;
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Create Account</h1>
        <p className="text-muted-foreground mt-2">
          Sign up for an admin account
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {registerError && (
          <div
            className="bg-destructive/10 text-destructive text-sm p-3 rounded-md"
            role="alert"
            aria-live="polite"
          >
            {registerError}
          </div>
        )}

        <div className="space-y-2">
          <label htmlFor="display_name" className="text-sm font-medium">
            Display Name
          </label>
          <input
            id="display_name"
            name="display_name"
            type="text"
            autoComplete="name"
            required
            value={formData.display_name}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary ${
              errors.display_name ? "border-destructive" : "border-input"
            }`}
            aria-invalid={!!errors.display_name}
            aria-describedby={errors.display_name ? "display_name-error" : undefined}
          />
          {errors.display_name && (
            <p id="display_name-error" className="text-sm text-destructive">
              {errors.display_name}
            </p>
          )}
        </div>

        <div className="space-y-2">
          <label htmlFor="email" className="text-sm font-medium">
            Email
          </label>
          <input
            id="email"
            name="email"
            type="email"
            autoComplete="email"
            required
            value={formData.email}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary ${
              errors.email ? "border-destructive" : "border-input"
            }`}
            aria-invalid={!!errors.email}
            aria-describedby={errors.email ? "email-error" : undefined}
          />
          {errors.email && (
            <p id="email-error" className="text-sm text-destructive">
              {errors.email}
            </p>
          )}
        </div>

        <div className="space-y-2">
          <label htmlFor="password" className="text-sm font-medium">
            Password
          </label>
          <input
            id="password"
            name="password"
            type="password"
            autoComplete="new-password"
            required
            value={formData.password}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary ${
              errors.password ? "border-destructive" : "border-input"
            }`}
            aria-invalid={!!errors.password}
            aria-describedby={errors.password ? "password-error" : undefined}
          />
          {errors.password && (
            <p id="password-error" className="text-sm text-destructive">
              {errors.password}
            </p>
          )}
          <p className="text-xs text-muted-foreground">
            Must be at least 8 characters with uppercase, lowercase, number, and special character
          </p>
        </div>

        <div className="space-y-2">
          <label htmlFor="confirm_password" className="text-sm font-medium">
            Confirm Password
          </label>
          <input
            id="confirm_password"
            name="confirm_password"
            type="password"
            autoComplete="new-password"
            required
            value={formData.confirm_password}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary ${
              errors.confirm_password ? "border-destructive" : "border-input"
            }`}
            aria-invalid={!!errors.confirm_password}
            aria-describedby={errors.confirm_password ? "confirm_password-error" : undefined}
          />
          {errors.confirm_password && (
            <p id="confirm_password-error" className="text-sm text-destructive">
              {errors.confirm_password}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full py-2 px-4 bg-primary text-primary-foreground rounded-md font-medium hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Creating account..." : "Create Account"}
        </button>
      </form>

      <div className="text-center text-sm">
        <span className="text-muted-foreground">Already have an account? </span>
        <Link
          href="/login"
          className="text-primary hover:underline font-medium"
        >
          Sign In
        </Link>
      </div>
    </div>
  );
}

function RegisterFallback() {
  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Create Account</h1>
        <p className="text-muted-foreground mt-2">
          Sign up for an admin account
        </p>
      </div>
      <div className="flex items-center justify-center py-8">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    </div>
  );
}

export default function RegisterPage() {
  return (
    <Suspense fallback={<RegisterFallback />}>
      <RegisterForm />
    </Suspense>
  );
}
