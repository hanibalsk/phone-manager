"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import type { ProximityAlert, CreateProximityAlertRequest } from "@/types";
import { proximityAlertsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { ProximityAlertForm } from "@/components/proximity";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { ArrowLeft, Users, RefreshCw } from "lucide-react";

interface Props {
  alertId: string;
}

export function EditProximityAlertClient({ alertId }: Props) {
  const router = useRouter();

  const [error, setError] = useState<string | null>(null);

  const {
    data: alert,
    loading: loadingAlert,
    error: fetchError,
    execute: fetchAlert,
  } = useApi<ProximityAlert>();
  const { loading: updating, execute: updateAlert } = useApi<ProximityAlert>();

  useEffect(() => {
    fetchAlert(() => proximityAlertsApi.get(alertId));
  }, [fetchAlert, alertId]);

  const handleSubmit = async (data: CreateProximityAlertRequest) => {
    setError(null);

    const result = await updateAlert(() => proximityAlertsApi.update(alertId, data));
    if (result) {
      router.push("/proximity-alerts");
    } else {
      setError("Failed to update proximity alert. Please try again.");
    }
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
          <Users className="h-8 w-8" />
          Edit Proximity Alert
        </h1>
        <p className="text-muted-foreground">
          Update alert configuration and trigger settings
        </p>
      </div>

      {/* Loading */}
      {loadingAlert && (
        <Card>
          <CardContent className="py-8">
            <div className="flex flex-col items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
              <p className="mt-2 text-muted-foreground">Loading alert...</p>
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

      {/* Form */}
      {alert && !loadingAlert && (
        <Card>
          <CardHeader>
            <CardTitle>Alert Details</CardTitle>
            <CardDescription>
              Update the configuration for &quot;{alert.name}&quot;
            </CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <div className="mb-4 p-3 bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 rounded-md">
                {error}
              </div>
            )}
            <ProximityAlertForm
              alert={alert}
              onSubmit={handleSubmit}
              loading={updating}
            />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
