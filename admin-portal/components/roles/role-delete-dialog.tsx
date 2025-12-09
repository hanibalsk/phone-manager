"use client";

import { useState, useId } from "react";
import type { Role } from "@/types";
import { rolesApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { X, Trash2, AlertCircle, AlertTriangle, Users } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface RoleDeleteDialogProps {
  role: Role;
  onSuccess: () => void;
  onCancel: () => void;
}

export function RoleDeleteDialog({ role, onSuccess, onCancel }: RoleDeleteDialogProps) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const hasUsers = role.user_count > 0;

  const handleDelete = async () => {
    if (hasUsers) {
      setError("Cannot delete a role that has users assigned. Please reassign users first.");
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      const result = await rolesApi.delete(role.id);

      if (result.error) {
        setError(result.error);
        return;
      }

      onSuccess();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete role");
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
            <Trash2 className="h-5 w-5" aria-hidden="true" />
            Delete Role
          </CardTitle>
          <CardDescription id={descriptionId}>
            This action cannot be undone
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          {error && (
            <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
              <AlertCircle className="h-4 w-4" />
              {error}
            </div>
          )}

          {hasUsers && (
            <div className="flex items-start gap-2 p-3 bg-warning/10 border border-warning/20 rounded-lg">
              <AlertTriangle className="h-4 w-4 text-warning mt-0.5" />
              <div className="text-sm">
                <p className="font-medium text-warning">Cannot delete this role</p>
                <p className="text-muted-foreground">
                  This role has {role.user_count} user{role.user_count !== 1 ? "s" : ""} assigned.
                  Reassign them to a different role before deleting.
                </p>
              </div>
            </div>
          )}

          <div className="p-4 bg-muted rounded-lg space-y-2">
            <p className="font-medium">{role.name}</p>
            <p className="text-sm text-muted-foreground">
              Code: {role.code.replace(/_/g, " ").toUpperCase()}
            </p>
            {role.description && (
              <p className="text-sm text-muted-foreground">{role.description}</p>
            )}
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Users className="h-4 w-4" />
              <span>{role.user_count} user{role.user_count !== 1 ? "s" : ""} assigned</span>
            </div>
          </div>

          {!hasUsers && (
            <p className="text-sm text-muted-foreground">
              Are you sure you want to delete this role? All permissions associated with this role will be removed.
            </p>
          )}
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
          <Button
            type="button"
            variant="destructive"
            onClick={handleDelete}
            disabled={submitting || hasUsers}
          >
            {submitting ? "Deleting..." : "Delete Role"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
