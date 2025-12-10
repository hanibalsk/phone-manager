"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { CreateProximityAlertRequest } from "@/types";
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
import { ArrowLeft, Users } from "lucide-react";

export default function NewProximityAlertPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const { loading, execute } = useApi();

  const handleSubmit = async (data: CreateProximityAlertRequest) => {
    setError(null);

    const result = await execute(() => proximityAlertsApi.create(data));
    if (result) {
      router.push("/proximity-alerts");
    } else {
      setError("Failed to create proximity alert. Please try again.");
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
          Create Proximity Alert
        </h1>
        <p className="text-muted-foreground">
          Set up a new alert to monitor distance between two devices
        </p>
      </div>

      {/* Form */}
      <Card>
        <CardHeader>
          <CardTitle>Alert Details</CardTitle>
          <CardDescription>
            Configure the device pair and trigger settings
          </CardDescription>
        </CardHeader>
        <CardContent>
          {error && (
            <div className="mb-4 p-3 bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 rounded-md">
              {error}
            </div>
          )}
          <ProximityAlertForm onSubmit={handleSubmit} loading={loading} />
        </CardContent>
      </Card>
    </div>
  );
}
