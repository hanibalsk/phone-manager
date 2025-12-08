"use client";

import type { PublicConfig } from "@/types";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { RefreshCw, Shield, Settings, Check, X } from "lucide-react";
import { Button } from "@/components/ui/button";

interface ConfigDisplayProps {
  config: PublicConfig | null;
  loading: boolean;
  onRefresh: () => void;
}

function StatusBadge({ enabled, label }: { enabled: boolean; label: string }) {
  return (
    <div className="flex items-center justify-between py-2">
      <span className="text-sm">{label}</span>
      <Badge variant={enabled ? "default" : "secondary"} className="flex items-center gap-1">
        {enabled ? <Check className="h-3 w-3" /> : <X className="h-3 w-3" />}
        {enabled ? "Enabled" : "Disabled"}
      </Badge>
    </div>
  );
}

export function ConfigDisplay({
  config,
  loading,
  onRefresh,
}: ConfigDisplayProps) {
  return (
    <div className="space-y-6">
      {/* Auth Configuration */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Shield className="h-5 w-5 text-muted-foreground" />
              <div>
                <CardTitle>Authentication Settings</CardTitle>
                <CardDescription>
                  User registration and login configuration
                </CardDescription>
              </div>
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={onRefresh}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : config ? (
            <div className="divide-y">
              <StatusBadge
                enabled={config.auth.registration_enabled}
                label="Email/Password Registration"
              />
              <StatusBadge
                enabled={config.auth.invite_only}
                label="Invite-Only Mode"
              />
              <StatusBadge
                enabled={config.auth.oauth_only}
                label="OAuth-Only Mode"
              />
              <StatusBadge
                enabled={config.auth.google_enabled}
                label="Google Sign-In"
              />
              <StatusBadge
                enabled={config.auth.apple_enabled}
                label="Apple Sign-In"
              />
            </div>
          ) : (
            <p className="text-sm text-muted-foreground text-center py-4">
              Unable to load configuration
            </p>
          )}
        </CardContent>
      </Card>

      {/* Feature Toggles */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Settings className="h-5 w-5 text-muted-foreground" />
            <div>
              <CardTitle>Feature Toggles</CardTitle>
              <CardDescription>
                Optional features that can be enabled or disabled
              </CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : config ? (
            <div className="divide-y">
              <StatusBadge
                enabled={config.features.geofences}
                label="Geofences"
              />
              <StatusBadge
                enabled={config.features.proximity_alerts}
                label="Proximity Alerts"
              />
              <StatusBadge
                enabled={config.features.webhooks}
                label="Webhooks"
              />
              <StatusBadge
                enabled={config.features.movement_tracking}
                label="Movement Tracking (Trips)"
              />
              <StatusBadge
                enabled={config.features.b2b}
                label="B2B / Organization Features"
              />
              <StatusBadge
                enabled={config.features.geofence_events}
                label="Geofence Events"
              />
            </div>
          ) : (
            <p className="text-sm text-muted-foreground text-center py-4">
              Unable to load configuration
            </p>
          )}
        </CardContent>
      </Card>

      {/* Configuration Note */}
      <Card className="border-dashed">
        <CardContent className="pt-6">
          <p className="text-sm text-muted-foreground">
            <strong>Note:</strong> These settings are configured via environment variables
            on the server. To change them, update the server configuration and restart.
            See the documentation for available environment variables.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
