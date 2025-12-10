"use client";

import { useState } from "react";
import type { EnrollmentToken, CreateEnrollmentTokenRequest } from "@/types";
import { enrollmentApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { RefreshCw, X, Plus } from "lucide-react";

interface CreateTokenDialogProps {
  onSuccess: (token: EnrollmentToken) => void;
  onCancel: () => void;
}

export function CreateTokenDialog({ onSuccess, onCancel }: CreateTokenDialogProps) {
  const [name, setName] = useState("");
  const [maxUses, setMaxUses] = useState<string>("");
  const [expiresAt, setExpiresAt] = useState("");

  const { loading, error, execute } = useApi<EnrollmentToken>();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const data: CreateEnrollmentTokenRequest = {
      name: name.trim(),
    };

    if (maxUses) {
      data.max_uses = parseInt(maxUses, 10);
    }

    if (expiresAt) {
      data.expires_at = new Date(expiresAt).toISOString();
    }

    const result = await execute(() => enrollmentApi.create(data));
    if (result) {
      onSuccess(result);
    }
  };

  const isValid = name.trim().length > 0;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onCancel}
      />

      {/* Dialog */}
      <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold">Create Enrollment Token</h2>
          <Button variant="ghost" size="sm" onClick={onCancel}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="text-sm font-medium">
              Token Name <span className="text-destructive">*</span>
            </label>
            <Input
              placeholder="e.g., Sales Team Onboarding"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1"
            />
          </div>

          <div>
            <label className="text-sm font-medium">Max Uses (optional)</label>
            <Input
              type="number"
              placeholder="Unlimited if empty"
              value={maxUses}
              onChange={(e) => setMaxUses(e.target.value)}
              min="1"
              className="mt-1"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Leave empty for unlimited uses
            </p>
          </div>

          <div>
            <label className="text-sm font-medium">Expiration Date (optional)</label>
            <Input
              type="datetime-local"
              value={expiresAt}
              onChange={(e) => setExpiresAt(e.target.value)}
              min={new Date().toISOString().slice(0, 16)}
              className="mt-1"
            />
            <p className="text-xs text-muted-foreground mt-1">
              Leave empty for no expiration
            </p>
          </div>

          {error && (
            <div className="p-3 rounded-md bg-destructive/10 text-destructive text-sm">
              {error}
            </div>
          )}

          <div className="flex justify-end gap-2 pt-4">
            <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>
              Cancel
            </Button>
            <Button type="submit" disabled={!isValid || loading}>
              {loading ? (
                <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Plus className="h-4 w-4 mr-2" />
              )}
              Create Token
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
