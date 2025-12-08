"use client";

import { Suspense, useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/contexts/auth-context";
import { loginSchema, getFieldErrors } from "@/lib/schemas";
import type { LoginInput } from "@/lib/schemas";
import { configApi } from "@/lib/api-client";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();

  const [formData, setFormData] = useState<LoginInput>({
    email: "",
    password: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);
  const [registrationEnabled, setRegistrationEnabled] = useState<boolean>(false);

  const returnUrl = searchParams.get("returnUrl") || "/";

  // Check if registration is enabled
  useEffect(() => {
    const checkConfig = async () => {
      const response = await configApi.getPublic();
      if (response.data) {
        setRegistrationEnabled(response.data.auth.registration_enabled);
      }
    };
    checkConfig();
  }, []);

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
    // Clear login error
    if (loginError) {
      setLoginError(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError(null);

    // Validate form
    const fieldErrors = getFieldErrors(loginSchema, formData);
    if (Object.keys(fieldErrors).length > 0) {
      setErrors(fieldErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await login(formData);
      if (result.success) {
        router.push(returnUrl);
      } else {
        setLoginError(result.error || "Invalid email or password");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Sign In</h1>
        <p className="text-muted-foreground mt-2">
          Sign in to your admin account
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {loginError && (
          <div
            className="bg-destructive/10 text-destructive text-sm p-3 rounded-md"
            role="alert"
            aria-live="polite"
          >
            {loginError}
          </div>
        )}

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
            autoComplete="current-password"
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
        </div>

        <div className="flex justify-end">
          <Link
            href="/forgot-password"
            className="text-sm text-primary hover:underline"
          >
            Forgot Password?
          </Link>
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full py-2 px-4 bg-primary text-primary-foreground rounded-md font-medium hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Signing in..." : "Sign In"}
        </button>
      </form>

      {registrationEnabled && (
        <div className="text-center text-sm">
          <span className="text-muted-foreground">Don&apos;t have an account? </span>
          <Link
            href="/register"
            className="text-primary hover:underline font-medium"
          >
            Create Account
          </Link>
        </div>
      )}
    </div>
  );
}

function LoginFallback() {
  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Sign In</h1>
        <p className="text-muted-foreground mt-2">
          Sign in to your admin account
        </p>
      </div>
      <div className="flex items-center justify-center py-8">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<LoginFallback />}>
      <LoginForm />
    </Suspense>
  );
}
