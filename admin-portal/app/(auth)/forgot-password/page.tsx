"use client";

import { useState } from "react";
import Link from "next/link";
import { authApi } from "@/lib/api-client";
import { forgotPasswordSchema, getFieldErrors } from "@/lib/schemas";
import type { ForgotPasswordInput } from "@/lib/schemas";

export default function ForgotPasswordPage() {
  const [formData, setFormData] = useState<ForgotPasswordInput>({
    email: "",
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);

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
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate form
    const fieldErrors = getFieldErrors(forgotPasswordSchema, formData);
    if (Object.keys(fieldErrors).length > 0) {
      setErrors(fieldErrors);
      return;
    }

    setIsSubmitting(true);
    try {
      // Always show success message for security (don't reveal if email exists)
      await authApi.forgotPassword({ email: formData.email });
      setIsSubmitted(true);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isSubmitted) {
    return (
      <div className="space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold">Check Your Email</h1>
          <p className="text-muted-foreground mt-2">
            If an account exists with that email, you will receive a password reset link.
          </p>
        </div>

        <div className="text-center">
          <Link
            href="/login"
            className="text-primary hover:underline"
          >
            Return to Sign In
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="text-center">
        <h1 className="text-2xl font-bold">Forgot Password</h1>
        <p className="text-muted-foreground mt-2">
          Enter your email to receive a password reset link
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
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

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full py-2 px-4 bg-primary text-primary-foreground rounded-md font-medium hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Sending..." : "Send Reset Link"}
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
