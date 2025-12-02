"use client";

import { useState } from "react";
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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <Card className="w-full max-w-md mx-4">
        <CardHeader className="relative">
          <Button
            variant="ghost"
            size="icon"
            className="absolute right-4 top-4"
            onClick={onCancel}
          >
            <X className="h-4 w-4" />
          </Button>
          <CardTitle className="flex items-center gap-2">
            {isApprove ? (
              <>
                <CheckCircle className="h-5 w-5 text-green-600" />
                Approve Request
              </>
            ) : (
              <>
                <XCircle className="h-5 w-5 text-red-600" />
                Deny Request
              </>
            )}
          </CardTitle>
          <CardDescription>
            {isApprove
              ? `Grant ${request.requestedDuration} minutes of unlock time`
              : "Deny this unlock request"}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="p-3 bg-muted rounded-lg">
            <p className="text-sm font-medium">{request.deviceName}</p>
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
