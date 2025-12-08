"use client";

import { useState, useId } from "react";
import type { UserRole, CreateUserRequest } from "@/types";
import { usersApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { X, UserPlus, AlertCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface UserCreateDialogProps {
  onSuccess: () => void;
  onCancel: () => void;
}

const ROLE_OPTIONS: { value: UserRole; label: string }[] = [
  { value: "viewer", label: "Viewer" },
  { value: "support", label: "Support" },
  { value: "org_manager", label: "Organization Manager" },
  { value: "org_admin", label: "Organization Admin" },
  { value: "super_admin", label: "Super Admin" },
];

interface FormErrors {
  email?: string;
  display_name?: string;
  general?: string;
}

export function UserCreateDialog({ onSuccess, onCancel }: UserCreateDialogProps) {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [role, setRole] = useState<UserRole>("viewer");
  const [sendWelcomeEmail, setSendWelcomeEmail] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    if (!email.trim()) {
      newErrors.email = "Email is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      newErrors.email = "Please enter a valid email address";
    }

    if (!displayName.trim()) {
      newErrors.display_name = "Display name is required";
    } else if (displayName.trim().length < 2) {
      newErrors.display_name = "Display name must be at least 2 characters";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    setErrors({});

    try {
      const data: CreateUserRequest = {
        email: email.trim(),
        display_name: displayName.trim(),
        role,
        send_welcome_email: sendWelcomeEmail,
      };

      const result = await usersApi.create(data);

      if (result.error) {
        setErrors({ general: result.error });
        return;
      }

      onSuccess();
    } catch (error) {
      setErrors({
        general: error instanceof Error ? error.message : "Failed to create user"
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-md mx-4">
        <form onSubmit={handleSubmit}>
          <CardHeader className="relative">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="absolute right-4 top-4"
              onClick={onCancel}
              aria-label="Close dialog"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </Button>
            <CardTitle id={titleId} className="flex items-center gap-2">
              <UserPlus className="h-5 w-5" aria-hidden="true" />
              Create User
            </CardTitle>
            <CardDescription id={descriptionId}>
              Add a new user to the platform
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {errors.general && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {errors.general}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">
                Email <span className="text-destructive">*</span>
              </Label>
              <Input
                id="email"
                type="email"
                placeholder="user@example.com"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (errors.email) setErrors({ ...errors, email: undefined });
                }}
                className={errors.email ? "border-destructive" : ""}
              />
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="displayName">
                Display Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="displayName"
                placeholder="John Doe"
                value={displayName}
                onChange={(e) => {
                  setDisplayName(e.target.value);
                  if (errors.display_name) setErrors({ ...errors, display_name: undefined });
                }}
                className={errors.display_name ? "border-destructive" : ""}
              />
              {errors.display_name && (
                <p className="text-sm text-destructive">{errors.display_name}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="role">Role</Label>
              <select
                id="role"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
              >
                {ROLE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="sendWelcomeEmail"
                checked={sendWelcomeEmail}
                onChange={(e) => setSendWelcomeEmail(e.target.checked)}
                className="rounded border-input"
              />
              <Label htmlFor="sendWelcomeEmail" className="font-normal cursor-pointer">
                Send welcome email with setup instructions
              </Label>
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={onCancel}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Creating..." : "Create User"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
