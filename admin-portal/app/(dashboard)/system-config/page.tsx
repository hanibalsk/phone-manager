"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Shield, Flag, Gauge, Key, Database, Settings } from "lucide-react";
import { AuthSettings, FeatureFlags, RateLimits, ApiKeys } from "@/components/system-config";

type ConfigTab =
  | "auth"
  | "features"
  | "rate-limits"
  | "api-keys"
  | "retention";

interface TabConfig {
  id: ConfigTab;
  label: string;
  icon: React.ElementType;
  description: string;
}

const tabs: TabConfig[] = [
  {
    id: "auth",
    label: "Authentication",
    icon: Shield,
    description: "Configure login, OAuth, and security settings",
  },
  {
    id: "features",
    label: "Feature Flags",
    icon: Flag,
    description: "Enable or disable platform features",
  },
  {
    id: "rate-limits",
    label: "Rate Limits",
    icon: Gauge,
    description: "Configure API rate limits and overrides",
  },
  {
    id: "api-keys",
    label: "API Keys",
    icon: Key,
    description: "Manage API keys for integrations",
  },
  {
    id: "retention",
    label: "Data Retention",
    icon: Database,
    description: "Configure data retention policies",
  },
];

export default function SystemConfigPage() {
  const [activeTab, setActiveTab] = useState<ConfigTab>("auth");

  const renderContent = () => {
    switch (activeTab) {
      case "auth":
        return <AuthSettings />;
      case "features":
        return <FeatureFlags />;
      case "rate-limits":
        return <RateLimits />;
      case "api-keys":
        return <ApiKeys />;
      case "retention":
        return (
          <div className="flex items-center justify-center h-64 text-muted-foreground">
            Data Retention - Coming in AP-9.5
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Settings className="h-8 w-8 text-primary" />
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            System Configuration
          </h1>
          <p className="text-muted-foreground">
            Manage platform settings and policies
          </p>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="border rounded-lg p-1 bg-muted/30">
        <div className="flex flex-wrap gap-1">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            return (
              <Button
                key={tab.id}
                variant={activeTab === tab.id ? "default" : "ghost"}
                size="sm"
                onClick={() => setActiveTab(tab.id)}
                className="flex items-center gap-2"
              >
                <Icon className="h-4 w-4" />
                {tab.label}
              </Button>
            );
          })}
        </div>
      </div>

      {/* Tab Description */}
      <div className="text-sm text-muted-foreground">
        {tabs.find((t) => t.id === activeTab)?.description}
      </div>

      {/* Content */}
      <div className="border rounded-lg p-6">{renderContent()}</div>
    </div>
  );
}
