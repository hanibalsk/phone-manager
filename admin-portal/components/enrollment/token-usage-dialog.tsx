"use client";

import { useState, useEffect, useCallback } from "react";
import type { EnrollmentToken, TokenUsage } from "@/types";
import { enrollmentApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { X, RefreshCw, Smartphone, Calendar } from "lucide-react";

interface TokenUsageDialogProps {
  token: EnrollmentToken;
  onClose: () => void;
}

export function TokenUsageDialog({ token, onClose }: TokenUsageDialogProps) {
  const { data, loading, error, execute } = useApi<{ enrollments: TokenUsage[] }>();

  const fetchUsage = useCallback(() => {
    execute(() => enrollmentApi.getUsage(token.id));
  }, [execute, token.id]);

  useEffect(() => {
    fetchUsage();
  }, [fetchUsage]);

  const enrollments = data?.enrollments || [];

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />

      {/* Dialog */}
      <div className="relative bg-background rounded-lg shadow-lg w-full max-w-lg mx-4 p-6 max-h-[80vh] overflow-hidden flex flex-col">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold">Token Usage History</h2>
            <p className="text-sm text-muted-foreground">{token.name}</p>
          </div>
          <Button variant="ghost" size="sm" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="flex-1 overflow-auto">
          {/* Loading State */}
          {loading && (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          )}

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={fetchUsage}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && enrollments.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Smartphone className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">
                No devices have been enrolled with this token yet
              </p>
            </div>
          )}

          {/* Usage List */}
          {!loading && !error && enrollments.length > 0 && (
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground mb-4">
                {enrollments.length} device{enrollments.length !== 1 ? "s" : ""} enrolled
              </p>
              {enrollments.map((enrollment) => (
                <div
                  key={enrollment.device_id}
                  className="flex items-center justify-between p-3 rounded-lg border"
                >
                  <div className="flex items-center gap-3">
                    <Smartphone className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <p className="font-medium">{enrollment.device_name}</p>
                      <p className="text-xs text-muted-foreground font-mono">
                        {enrollment.device_id.slice(0, 8)}...
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Calendar className="h-4 w-4" />
                    {formatDateTime(enrollment.enrolled_at)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className="pt-4 border-t mt-4">
          <Button variant="outline" className="w-full" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}
