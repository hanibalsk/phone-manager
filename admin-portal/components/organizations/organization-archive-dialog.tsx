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
import { X, Archive, AlertCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface OrganizationArchiveDialogProps {
  organization: Organization;
  onSuccess: () => void;
  onCancel: () => void;
}

export function OrganizationArchiveDialog({ organization, onSuccess, onCancel }: OrganizationArchiveDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    setSubmitting(true);
    setError(null);

    try {
      const result = await organizationsApi.archive(organization.id);

      if (result.error) {
        setError(result.error);
        return;
      }

      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to archive organization");
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
              <Archive className="h-5 w-5" aria-hidden="true" />
              Archive Organization
            </CardTitle>
            <CardDescription id={descriptionId}>
              Archive {organization.name}. This action can be undone.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {error && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {error}
              </div>
            )}

            <div className="p-3 bg-muted rounded-lg text-sm">
              <p className="font-medium mb-1">What happens when you archive?</p>
              <ul className="list-disc list-inside text-muted-foreground space-y-1">
                <li>Organization will be hidden from active lists</li>
                <li>All users will lose access</li>
                <li>Data will be preserved for future reference</li>
                <li>You can restore this organization later</li>
              </ul>
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
            <Button type="submit" variant="secondary" disabled={submitting}>
              {submitting ? "Archiving..." : "Archive Organization"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
