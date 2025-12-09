"use client";

import { useState, useId } from "react";
import type { Organization } from "@/types";
import { organizationsApi } from "@/lib/api-client";
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
import { Textarea } from "@/components/ui/textarea";
import { X, Ban, AlertCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface OrganizationSuspendDialogProps {
  organization: Organization;
  onSuccess: () => void;
  onCancel: () => void;
}

export function OrganizationSuspendDialog({ organization, onSuccess, onCancel }: OrganizationSuspendDialogProps) {
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
      const result = await organizationsApi.suspend(organization.id, reason.trim());

      if (result.error) {
        setError(result.error);
        return;
      }

      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to suspend organization");
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
              <Ban className="h-5 w-5" aria-hidden="true" />
              Suspend Organization
            </CardTitle>
            <CardDescription id={descriptionId}>
              Suspend {organization.name}. All users will lose access until reactivated.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {error}
              </div>
            )}

            <div className="p-3 bg-destructive/10 rounded-lg text-sm">
              <p className="font-medium text-destructive mb-1">Warning</p>
              <p className="text-muted-foreground">
                Suspending this organization will immediately revoke access for all users.
                Devices will continue to function but administrative actions will be blocked.
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="reason">
                Reason for Suspension <span className="text-destructive">*</span>
              </Label>
              <Textarea
                id="reason"
                placeholder="Explain why this organization is being suspended..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                rows={3}
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
              {submitting ? "Suspending..." : "Suspend Organization"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
