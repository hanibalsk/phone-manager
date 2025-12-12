"use client";

import { useState, useEffect, useId, useCallback } from "react";
import type { AdminUser, MfaStatus } from "@/types";
import { usersApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { X, Shield, RefreshCw, AlertCircle, CheckCircle, AlertTriangle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface UserMfaDialogProps {
  user: AdminUser;
  onClose: () => void;
}

export function UserMfaDialog({ user, onClose }: UserMfaDialogProps) {
  const [mfaStatus, setMfaStatus] = useState<MfaStatus | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onClose });
  const titleId = useId();
  const descriptionId = useId();

  const fetchMfaStatus = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await usersApi.getMfaStatus(user.id);
      if (result.error) {
        setError(result.error);
      } else if (result.data) {
        setMfaStatus(result.data);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load MFA status");
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    fetchMfaStatus();
  }, [fetchMfaStatus]);

  const handleForceEnrollment = async () => {
    setActionLoading("force");
    setError(null);
    setSuccessMessage(null);
    try {
      const result = await usersApi.forceMfaEnrollment(user.id);
      if (result.error) {
        setError(result.error);
      } else {
        setSuccessMessage("MFA enrollment requirement set. User will be prompted on next login.");
        await fetchMfaStatus();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to force MFA enrollment");
    } finally {
      setActionLoading(null);
    }
  };

  const handleResetMfa = async () => {
    setActionLoading("reset");
    setError(null);
    setSuccessMessage(null);
    try {
      const result = await usersApi.resetMfa(user.id);
      if (result.error) {
        setError(result.error);
      } else {
        setSuccessMessage("MFA has been reset. User will need to re-enroll.");
        await fetchMfaStatus();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to reset MFA");
    } finally {
      setActionLoading(null);
    }
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const getMethodLabel = (method: string | null) => {
    switch (method) {
      case "totp":
        return "Authenticator App";
      case "sms":
        return "SMS";
      case "email":
        return "Email";
      default:
        return "Not configured";
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
            onClick={onClose}
            aria-label="Close dialog"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </Button>
          <CardTitle id={titleId} className="flex items-center gap-2">
            <Shield className="h-5 w-5" aria-hidden="true" />
            MFA Settings
          </CardTitle>
          <CardDescription id={descriptionId}>
            Manage MFA for {user.display_name}
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4">
          {error && (
            <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
              <AlertCircle className="h-4 w-4" />
              {error}
            </div>
          )}

          {successMessage && (
            <div className="flex items-center gap-2 p-3 text-sm text-green-600 bg-green-50 rounded-lg">
              <CheckCircle className="h-4 w-4" />
              {successMessage}
            </div>
          )}

          {loading && (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          )}

          {!loading && mfaStatus && (
            <>
              <div className="p-4 bg-muted rounded-lg space-y-3">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium">Status</span>
                  {mfaStatus.enabled ? (
                    <Badge variant="success">Enabled</Badge>
                  ) : (
                    <Badge variant="secondary">Disabled</Badge>
                  )}
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted-foreground">Method</span>
                  <span className="text-sm">{getMethodLabel(mfaStatus.method)}</span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-sm text-muted-foreground">Enrolled</span>
                  <span className="text-sm">{formatDate(mfaStatus.enrolled_at)}</span>
                </div>

                {mfaStatus.enabled && (
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted-foreground">Backup codes</span>
                    <span className={`text-sm ${mfaStatus.backup_codes_remaining < 3 ? "text-warning" : ""}`}>
                      {mfaStatus.backup_codes_remaining} remaining
                    </span>
                  </div>
                )}
              </div>

              {!mfaStatus.enabled && (
                <div className="flex items-start gap-2 p-3 bg-warning/10 border border-warning/20 rounded-lg">
                  <AlertTriangle className="h-4 w-4 text-warning mt-0.5" />
                  <p className="text-sm text-warning">
                    MFA is not enabled. Consider requiring enrollment for security.
                  </p>
                </div>
              )}
            </>
          )}
        </CardContent>

        <CardFooter className="flex justify-between gap-2">
          <div className="flex gap-2">
            {!mfaStatus?.enabled && (
              <Button
                size="sm"
                onClick={handleForceEnrollment}
                disabled={actionLoading !== null || loading}
              >
                {actionLoading === "force" ? "Processing..." : "Force Enrollment"}
              </Button>
            )}

            {mfaStatus?.enabled && (
              <Button
                variant="destructive"
                size="sm"
                onClick={handleResetMfa}
                disabled={actionLoading !== null || loading}
              >
                {actionLoading === "reset" ? "Resetting..." : "Reset MFA"}
              </Button>
            )}
          </div>

          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
