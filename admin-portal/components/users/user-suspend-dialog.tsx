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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { X, UserX, AlertCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface UserSuspendDialogProps {
  user: AdminUser;
  onSuccess: () => void;
  onCancel: () => void;
}

export function UserSuspendDialog({ user, onSuccess, onCancel }: UserSuspendDialogProps) {
  const [reason, setReason] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!reason.trim()) {
      setError("Please provide a reason for suspension");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const result = await usersApi.suspend(user.id, reason.trim());

      if (result.error) {
        setError(result.error);
        return;
      }

      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to suspend user");
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
            <CardTitle id={titleId} className="flex items-center gap-2 text-destructive">
              <UserX className="h-5 w-5" aria-hidden="true" />
              Suspend User
            </CardTitle>
            <CardDescription id={descriptionId}>
              Suspend access for {user.display_name}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {error}
              </div>
            )}

            <div className="p-3 bg-muted rounded-lg">
              <p className="text-sm font-medium">{user.email}</p>
              <p className="text-sm text-muted-foreground">{user.display_name}</p>
            </div>

            <div className="p-3 bg-destructive/10 border border-destructive/20 rounded-lg">
              <p className="text-sm text-destructive">
                This will immediately revoke the user's access and terminate any active sessions.
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="reason">
                Reason for suspension <span className="text-destructive">*</span>
              </Label>
              <Input
                id="reason"
                placeholder="e.g., Policy violation, requested by HR..."
                value={reason}
                onChange={(e) => {
                  setReason(e.target.value);
                  if (error) setError(null);
                }}
              />
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
            <Button type="submit" variant="destructive" disabled={submitting}>
              {submitting ? "Suspending..." : "Suspend User"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
