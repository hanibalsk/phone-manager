"use client";

import { useState } from "react";
import type { AdminUnlockRequest } from "@/types";
import { unlockRequestsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { CheckCircle, XCircle, Clock } from "lucide-react";

const DURATION_OPTIONS = [
  { value: 15, label: "15 minutes" },
  { value: 30, label: "30 minutes" },
  { value: 60, label: "1 hour" },
  { value: 120, label: "2 hours" },
  { value: 0, label: "Custom" },
];

interface RequestActionModalProps {
  request: AdminUnlockRequest;
  action: "approve" | "deny";
  onClose: () => void;
  onComplete: (success: boolean, message: string) => void;
}

export function RequestActionModal({
  request,
  action,
  onClose,
  onComplete,
}: RequestActionModalProps) {
  const [selectedDuration, setSelectedDuration] = useState<number>(request.requested_duration_minutes);
  const [customDuration, setCustomDuration] = useState<string>("");
  const [denyNote, setDenyNote] = useState("");
  const [useCustom, setUseCustom] = useState(false);

  const { execute: approveRequest, loading: approving } = useApi<AdminUnlockRequest>();
  const { execute: denyRequest, loading: denying } = useApi<AdminUnlockRequest>();

  const handleApprove = async () => {
    const duration = useCustom ? parseInt(customDuration) : selectedDuration;
    if (!duration || duration <= 0) {
      onComplete(false, "Please select a valid duration");
      return;
    }

    const result = await approveRequest(() =>
      unlockRequestsApi.approve(request.id, { duration_minutes: duration })
    );

    if (result) {
      onComplete(true, "Request approved successfully");
    } else {
      onComplete(false, "Failed to approve request");
    }
  };

  const handleDeny = async () => {
    if (!denyNote.trim()) {
      onComplete(false, "Please provide a reason for denial");
      return;
    }

    const result = await denyRequest(() =>
      unlockRequestsApi.deny(request.id, { note: denyNote.trim() })
    );

    if (result) {
      onComplete(true, "Request denied");
    } else {
      onComplete(false, "Failed to deny request");
    }
  };

  const formatDuration = (minutes: number) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  const isApprove = action === "approve";
  const loading = approving || denying;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-modal="true">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
        <div className="flex items-center gap-3 mb-4">
          <div className={`rounded-full p-2 ${isApprove ? "bg-green-100" : "bg-red-100"}`}>
            {isApprove ? (
              <CheckCircle className="h-5 w-5 text-green-600" />
            ) : (
              <XCircle className="h-5 w-5 text-red-600" />
            )}
          </div>
          <h3 className="text-lg font-semibold">
            {isApprove ? "Approve Request" : "Deny Request"}
          </h3>
        </div>

        {/* Request Summary */}
        <div className="mb-4 p-3 bg-secondary/50 rounded-lg text-sm">
          <div className="font-medium">{request.device_name}</div>
          <div className="text-muted-foreground">{request.user_name}</div>
          <div className="flex items-center gap-1 mt-1 text-muted-foreground">
            <Clock className="h-3 w-3" />
            Requested: {formatDuration(request.requested_duration_minutes)}
          </div>
          {request.reason && (
            <div className="mt-2 text-muted-foreground">
              &ldquo;{request.reason}&rdquo;
            </div>
          )}
        </div>

        {isApprove ? (
          <>
            {/* Duration Selection */}
            <div className="mb-4">
              <label className="text-sm font-medium">Approval Duration</label>
              <div className="mt-2 grid grid-cols-3 gap-2">
                {DURATION_OPTIONS.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`p-2 text-sm rounded border transition-colors ${
                      !useCustom && selectedDuration === option.value
                        ? "border-primary bg-primary/10"
                        : "border-border hover:border-primary/50"
                    } ${option.value === 0 && useCustom ? "border-primary bg-primary/10" : ""}`}
                    onClick={() => {
                      if (option.value === 0) {
                        setUseCustom(true);
                      } else {
                        setUseCustom(false);
                        setSelectedDuration(option.value);
                      }
                    }}
                  >
                    {option.label}
                  </button>
                ))}
              </div>

              {useCustom && (
                <div className="mt-3">
                  <label className="text-sm font-medium">Custom Duration (minutes)</label>
                  <Input
                    type="number"
                    min="1"
                    value={customDuration}
                    onChange={(e) => setCustomDuration(e.target.value)}
                    placeholder="Enter minutes"
                    className="mt-1"
                  />
                </div>
              )}
            </div>

            {/* Match Requested Duration Hint */}
            {request.requested_duration_minutes !== selectedDuration && !useCustom && (
              <div className="mb-4 text-sm text-muted-foreground">
                <button
                  type="button"
                  className="text-primary hover:underline"
                  onClick={() => setSelectedDuration(request.requested_duration_minutes)}
                >
                  Match requested duration ({formatDuration(request.requested_duration_minutes)})
                </button>
              </div>
            )}
          </>
        ) : (
          <>
            {/* Deny Note */}
            <div className="mb-4">
              <label className="text-sm font-medium">Reason for Denial *</label>
              <textarea
                value={denyNote}
                onChange={(e) => setDenyNote(e.target.value)}
                placeholder="Explain why this request is being denied..."
                className="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm min-h-[100px]"
              />
            </div>

            {/* Quick deny reasons */}
            <div className="mb-4">
              <div className="text-sm text-muted-foreground mb-2">Quick reasons:</div>
              <div className="flex flex-wrap gap-2">
                {[
                  "Outside allowed hours",
                  "Too many requests today",
                  "Homework not completed",
                  "Exceeded daily limit",
                ].map((reason) => (
                  <button
                    key={reason}
                    type="button"
                    className="text-xs px-2 py-1 rounded bg-secondary hover:bg-secondary/80"
                    onClick={() => setDenyNote(reason)}
                  >
                    {reason}
                  </button>
                ))}
              </div>
            </div>
          </>
        )}

        {/* Actions */}
        <div className="flex justify-end gap-3">
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          {isApprove ? (
            <Button onClick={handleApprove} disabled={loading}>
              {loading ? "Approving..." : "Approve"}
            </Button>
          ) : (
            <Button variant="destructive" onClick={handleDeny} disabled={loading}>
              {loading ? "Denying..." : "Deny"}
            </Button>
          )}
        </div>
      </div>
    </div>
  );
}
