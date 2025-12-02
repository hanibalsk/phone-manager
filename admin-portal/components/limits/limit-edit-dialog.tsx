"use client";

import { useState } from "react";
import type { DailyLimit } from "@/types";
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
import { X } from "lucide-react";

interface LimitEditDialogProps {
  limit: DailyLimit | null;
  onSave: (data: Omit<DailyLimit, "id">) => Promise<void>;
  onCancel: () => void;
}

export function LimitEditDialog({
  limit,
  onSave,
  onCancel,
}: LimitEditDialogProps) {
  const [packageName, setPackageName] = useState(limit?.packageName || "");
  const [appName, setAppName] = useState(limit?.appName || "");
  const [dailyLimitMinutes, setDailyLimitMinutes] = useState(
    limit?.dailyLimitMinutes || 60
  );
  const [enabled, setEnabled] = useState(limit?.enabled ?? true);
  const [submitting, setSubmitting] = useState(false);

  const isEditing = limit !== null;

  const handleSubmit = async () => {
    setSubmitting(true);
    try {
      await onSave({
        packageName,
        appName,
        dailyLimitMinutes,
        enabled,
      });
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
          <CardTitle>{isEditing ? "Edit Limit" : "Add Limit"}</CardTitle>
          <CardDescription>
            {isEditing
              ? "Modify the daily time limit for this app"
              : "Set a daily time limit for an app"}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="packageName">Package Name</Label>
            <Input
              id="packageName"
              placeholder="com.example.app"
              value={packageName}
              onChange={(e) => setPackageName(e.target.value)}
              disabled={isEditing}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="appName">App Name</Label>
            <Input
              id="appName"
              placeholder="Example App"
              value={appName}
              onChange={(e) => setAppName(e.target.value)}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="dailyLimit">Daily Limit (minutes)</Label>
            <Input
              id="dailyLimit"
              type="number"
              min={1}
              max={1440}
              value={dailyLimitMinutes}
              onChange={(e) => setDailyLimitMinutes(parseInt(e.target.value))}
            />
          </div>
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="enabled"
              checked={enabled}
              onChange={(e) => setEnabled(e.target.checked)}
              className="h-4 w-4"
            />
            <Label htmlFor="enabled">Enable this limit</Label>
          </div>
        </CardContent>
        <CardFooter className="flex justify-end gap-2">
          <Button variant="outline" onClick={onCancel} disabled={submitting}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={submitting || !packageName || !appName}
          >
            {submitting ? "Saving..." : isEditing ? "Save Changes" : "Add Limit"}
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
