"use client";

import { Suspense, useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { authApi } from "@/lib/api-client";
import { resetPasswordSchema, getFieldErrors } from "@/lib/schemas";

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get("token");

  const [formData, setFormData] = useState({
    new_password: "",
    confirm_password: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);

  useEffect(() => {
    if (!token) {
      setSubmitError("Invalid or missing reset token");
    }
  }, [token]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => {
        const newErrors = { ...prev };
        delete newErrors[name];
        return newErrors;
      });
    }
    if (submitError) {
      setSubmitError(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitError(null);

    if (!token) {
      setSubmitError("Invalid or missing reset token");
      return;
    }

    // Check password confirmation
    if (formData.new_password !== formData.confirm_password) {
      setErrors({ confirm_password: "Passwords do not match" });
      return;
    }

    // Validate form
    const fieldErrors = getFieldErrors(resetPasswordSchema, {
      token,
      new_password: formData.new_password,
    });
    if (Object.keys(fieldErrors).length > 0) {
      setErrors(fieldErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      const result = await authApi.resetPassword({
        token,
        new_password: formData.new_password,
      });
      if (result.error) {
        setSubmitError(result.error);
      } else {
        setIsSuccess(true);
        // Redirect to login after 3 seconds
        setTimeout(() => {
          router.push("/login");
        }, 3000);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSuccess) {
    return (
      <div className="space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold">Password Reset Successful</h1>
          <p className="text-muted-foreground mt-2">
            Your password has been reset. You will be redirected to the login page.
          </p>
        </div>

        <div className="text-center">
          <Link
            href="/login"
            className="text-primary hover:underline"
          >
            Go to Sign In
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Reset Password</h1>
        <p className="text-muted-foreground mt-2">
          Enter your new password
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        {submitError && (
          <div
            className="bg-destructive/10 text-destructive text-sm p-3 rounded-md"
            role="alert"
            aria-live="polite"
          >
            {submitError}
          </div>
        )}

        <div className="space-y-2">
          <label htmlFor="new_password" className="text-sm font-medium">
            New Password
          </label>
          <input
            id="new_password"
            name="new_password"
            type="password"
            autoComplete="new-password"
            required
            value={formData.new_password}
            onChange={handleChange}
            className={`w-full px-3 py-2 border rounded-md bg-background focus:outline-none focus:ring-2 focus:ring-primary ${
              errors.new_password ? "border-destructive" : "border-input"
            }`}
            aria-invalid={!!errors.new_password}
            aria-describedby={errors.new_password ? "new-password-error" : undefined}
          />
          {errors.new_password && (
            <p id="new-password-error" className="text-sm text-destructive">
              {errors.new_password}
            </p>
          )}
          <p className="text-xs text-muted-foreground">
            Must be at least 8 characters with uppercase, lowercase, and a number
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
            aria-describedby={errors.confirm_password ? "confirm-password-error" : undefined}
          />
          {errors.confirm_password && (
            <p id="confirm-password-error" className="text-sm text-destructive">
              {errors.confirm_password}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting || !token}
          className="w-full py-2 px-4 bg-primary text-primary-foreground rounded-md font-medium hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Resetting..." : "Reset Password"}
        </button>

        <div className="text-center">
          <Link
            href="/login"
            className="text-sm text-primary hover:underline"
          >
            Back to Sign In
          </Link>
        </div>
      </form>
    </div>
  );
}

function ResetPasswordFallback() {
  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Reset Password</h1>
        <p className="text-muted-foreground mt-2">
          Enter your new password
        </p>
      </div>
      <div className="flex items-center justify-center py-8">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    </div>
  );
}

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<ResetPasswordFallback />}>
      <ResetPasswordForm />
    </Suspense>
  );
}
