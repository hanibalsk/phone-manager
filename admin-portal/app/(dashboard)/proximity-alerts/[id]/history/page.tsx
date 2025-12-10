"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import type { ProximityAlert } from "@/types";
import { proximityAlertsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { AlertTriggerHistory } from "@/components/proximity";
import {
  Card,
  CardContent,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { ArrowLeft, History, RefreshCw, Users, Ruler, Clock } from "lucide-react";

export default function ProximityAlertHistoryPage() {
  const params = useParams();
  const alertId = params.id as string;

  const {
    data: alert,
    loading: loadingAlert,
    error: fetchError,
    execute: fetchAlert,
  } = useApi<ProximityAlert>();

  useEffect(() => {
    fetchAlert(() => proximityAlertsApi.get(alertId));
  }, [fetchAlert, alertId]);

  const formatDistance = (meters: number) => {
    if (meters >= 1000) {
      return `${(meters / 1000).toFixed(1)} km`;
    }
    return `${meters} m`;
  };

  const formatCooldown = (seconds: number) => {
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link href="/proximity-alerts">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Alerts
          </Button>
        </Link>
      </div>

      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <History className="h-8 w-8" />
          Trigger History
        </h1>
        <p className="text-muted-foreground">
          View all trigger events for this proximity alert
        </p>
      </div>

      {/* Loading */}
      {loadingAlert && (
        <Card>
          <CardContent className="py-8">
            <div className="flex flex-col items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
              <p className="mt-2 text-muted-foreground">Loading alert details...</p>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Error */}
      {fetchError && (
        <Card>
          <CardContent className="py-8">
            <div className="flex flex-col items-center justify-center text-center">
              <p className="text-destructive mb-4">{fetchError}</p>
              <Button
                variant="outline"
                onClick={() => fetchAlert(() => proximityAlertsApi.get(alertId))}
              >
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Alert Info & History */}
      {alert && !loadingAlert && (
        <>
          {/* Alert Summary Card */}
          <Card>
            <CardContent className="pt-6">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                  <p className="text-sm text-muted-foreground">Alert Name</p>
                  <p className="font-medium">{alert.name}</p>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Device Pair</p>
                  <div className="flex items-center gap-1">
                    <Users className="h-4 w-4 text-muted-foreground" />
                    <span className="text-sm">
                      {alert.device_a_name} ↔ {alert.device_b_name}
                    </span>
                  </div>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Trigger Distance</p>
                  <div className="flex items-center gap-1">
                    <Ruler className="h-4 w-4 text-muted-foreground" />
                    <span>{formatDistance(alert.trigger_distance_meters)}</span>
                  </div>
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Cooldown</p>
                  <div className="flex items-center gap-1">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <span>{formatCooldown(alert.cooldown_seconds)}</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Trigger History */}
          <AlertTriggerHistory alertId={alertId} alertName={alert.name} />
        </>
      )}
    </div>
  );
}
