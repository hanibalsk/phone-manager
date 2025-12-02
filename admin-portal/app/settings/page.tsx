"use client";

import { useEffect, useCallback } from "react";
import { Header } from "@/components/layout";
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
    await settingsApi.update(data);
    fetchSettings();
  };

  return (
    <div className="flex flex-col">
      <Header title="Settings" />
      <div className="p-6">
        <SettingsForm
          settings={settings}
          loading={loading}
          onRefresh={fetchSettings}
          onSave={handleSave}
        />
      </div>
    </div>
  );
}
