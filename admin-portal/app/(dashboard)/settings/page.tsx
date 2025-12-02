"use client";

import { useEffect, useCallback } from "react";
import { toast } from "sonner";
import { SettingsForm } from "@/components/settings";
import { settingsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import type { AdminSettings } from "@/types";

export default function SettingsPage() {
  const { data: settings, loading, execute } = useApi<AdminSettings>();

  const fetchSettings = useCallback(() => {
    execute(() => settingsApi.get());
  }, [execute]);

  useEffect(() => {
    fetchSettings();
  }, [fetchSettings]);

  const handleSave = async (data: Partial<AdminSettings>) => {
    try {
      const result = await settingsApi.update(data);
      if (result.error) {
        toast.error("Failed to save settings", {
          description: result.error,
        });
        return;
      }
      toast.success("Settings saved successfully");
      fetchSettings();
    } catch (error) {
      toast.error("Failed to save settings", {
        description: error instanceof Error ? error.message : "An unexpected error occurred",
      });
    }
  };

  return (
    <div className="p-6">
      <SettingsForm
        settings={settings}
        loading={loading}
        onRefresh={fetchSettings}
        onSave={handleSave}
      />
    </div>
  );
}
