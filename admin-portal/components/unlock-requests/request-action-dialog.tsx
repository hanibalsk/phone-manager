"use client";

import { useState, useId } from "react";
import type { UnlockRequest } from "@/types";
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
import { X, CheckCircle, XCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface RequestActionDialogProps {
  request: UnlockRequest;
  action: "approve" | "deny";
  onConfirm: (response?: string) => void;
  onCancel: () => void;
}

export function RequestActionDialog({
  request,
  action,
  onConfirm,
  onCancel,
}: RequestActionDialogProps) {
  const [response, setResponse] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const isApprove = action === "approve";

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      await onConfirm(response || undefined);
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
            variant="ghost"
            size="icon"
            className="absolute right-4 top-4"
            onClick={onCancel}
            aria-label="Close dialog"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </Button>
          <CardTitle id={titleId} className="flex items-center gap-2">
            {isApprove ? (
              <>
                <CheckCircle className="h-5 w-5 text-green-600" aria-hidden="true" />
                Approve Request
              </>
            ) : (
              <>
                <XCircle className="h-5 w-5 text-red-600" aria-hidden="true" />
                Deny Request
              </>
            )}
          </CardTitle>
          <CardDescription id={descriptionId}>
            {isApprove
              ? `Grant ${request.requested_duration} minutes of unlock time`
              : "Deny this unlock request"}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="p-3 bg-muted rounded-lg">
            <p className="text-sm font-medium">{request.device_name}</p>
            <p className="text-sm text-muted-foreground">{request.reason}</p>
          </div>
          <div className="space-y-2">
            <Label htmlFor="response">
              {isApprove ? "Message (optional)" : "Reason (optional)"}
            </Label>
            <Input
              id="response"
              placeholder={
                isApprove
                  ? "e.g., Enjoy your break!"
                  : "e.g., You've had enough screen time today"
              }
              value={response}
              onChange={(e) => setResponse(e.target.value)}
            />
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel} disabled={submitting}>
            Cancel
          </Button>
          <Button
            variant={isApprove ? "default" : "destructive"}
            onClick={handleSubmit}
            disabled={submitting}
          >
            {submitting ? "Processing..." : isApprove ? "Approve" : "Deny"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
