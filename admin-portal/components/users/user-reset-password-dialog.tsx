"use client";

import { useState, useId } from "react";
import type { AdminUser } from "@/types";
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
import { Label } from "@/components/ui/label";
import { X, KeyRound, AlertCircle, CheckCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface UserResetPasswordDialogProps {
  user: AdminUser;
  onSuccess: () => void;
  onCancel: () => void;
}

export function UserResetPasswordDialog({ user, onSuccess, onCancel }: UserResetPasswordDialogProps) {
  const [forceChange, setForceChange] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const result = await usersApi.resetPassword(user.id, forceChange);

      if (result.error) {
        setError(result.error);
        return;
      }

      setSuccess(true);
      setTimeout(() => {
        onSuccess();
      }, 1500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to reset password");
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
              <KeyRound className="h-5 w-5" aria-hidden="true" />
              Reset Password
            </CardTitle>
            <CardDescription id={descriptionId}>
              Send password reset email to {user.display_name}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {error}
              </div>
            )}

            {success && (
              <div className="flex items-center gap-2 p-3 text-sm text-green-600 bg-green-50 rounded-lg">
                <CheckCircle className="h-4 w-4" />
                Password reset email sent successfully!
              </div>
            )}

            <div className="p-3 bg-muted rounded-lg">
              <p className="text-sm font-medium">{user.email}</p>
              <p className="text-sm text-muted-foreground">{user.display_name}</p>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="forceChange"
                checked={forceChange}
                onChange={(e) => setForceChange(e.target.checked)}
                className="rounded border-input"
              />
              <Label htmlFor="forceChange" className="font-normal cursor-pointer">
                Force password change on next login
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
            <Button type="submit" disabled={submitting || success}>
              {submitting ? "Sending..." : "Send Reset Email"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
