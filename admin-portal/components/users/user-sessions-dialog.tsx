"use client";

import { useState, useEffect, useId, useCallback } from "react";
import type { AdminUser, UserSession } from "@/types";
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
import { X, Monitor, Trash2, RefreshCw, AlertCircle, Globe } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface UserSessionsDialogProps {
  user: AdminUser;
  onClose: () => void;
}

export function UserSessionsDialog({ user, onClose }: UserSessionsDialogProps) {
  const [sessions, setSessions] = useState<UserSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revoking, setRevoking] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onClose });
  const titleId = useId();
  const descriptionId = useId();

  const fetchSessions = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await usersApi.getSessions(user.id);
      if (result.error) {
        setError(result.error);
      } else if (result.data) {
        setSessions(result.data);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load sessions");
    } finally {
      setLoading(false);
    }
  }, [user.id]);

  useEffect(() => {
    fetchSessions();
  }, [fetchSessions]);

  const handleRevokeSession = async (sessionId: string) => {
    setRevoking(sessionId);
    try {
      const result = await usersApi.revokeSession(user.id, sessionId);
      if (!result.error) {
        setSessions(sessions.filter((s) => s.id !== sessionId));
      }
    } finally {
      setRevoking(null);
    }
  };

  const handleRevokeAll = async () => {
    setRevoking("all");
    try {
      const result = await usersApi.revokeAllSessions(user.id);
      if (!result.error) {
        setSessions([]);
      }
    } finally {
      setRevoking(null);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-lg mx-4 max-h-[80vh] flex flex-col">
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
            <Monitor className="h-5 w-5" aria-hidden="true" />
            Active Sessions
          </CardTitle>
          <CardDescription id={descriptionId}>
            {user.display_name}&apos;s active sessions
          </CardDescription>
        </CardHeader>

        <CardContent className="flex-1 overflow-auto space-y-3">
          {error && (
            <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
              <AlertCircle className="h-4 w-4" />
              {error}
            </div>
          )}

          {loading && (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          )}

          {!loading && sessions.length === 0 && (
            <div className="text-center py-8 text-muted-foreground">
              No active sessions
            </div>
          )}

          {!loading && sessions.map((session) => (
            <div
              key={session.id}
              className="flex items-start justify-between p-3 bg-muted rounded-lg"
            >
              <div className="flex-1">
                <div className="flex items-center gap-2">
                  <span className="font-medium text-sm">{session.device}</span>
                  {session.is_current && (
                    <span className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded">
                      Current
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 mt-1 text-xs text-muted-foreground">
                  <Globe className="h-3 w-3" />
                  <span>{session.ip_address}</span>
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  Last activity: {formatDate(session.last_activity)}
                </p>
              </div>
              <Button
                variant="ghost"
                size="sm"
                className="text-destructive hover:text-destructive"
                onClick={() => handleRevokeSession(session.id)}
                disabled={revoking !== null}
              >
                {revoking === session.id ? (
                  <RefreshCw className="h-4 w-4 animate-spin" />
                ) : (
                  <Trash2 className="h-4 w-4" />
                )}
              </Button>
            </div>
          ))}
        </CardContent>

        <CardFooter className="flex justify-between border-t pt-4">
          <Button
            variant="destructive"
            size="sm"
            onClick={handleRevokeAll}
            disabled={revoking !== null || sessions.length === 0}
          >
            {revoking === "all" ? "Revoking..." : "Revoke All Sessions"}
          </Button>
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
