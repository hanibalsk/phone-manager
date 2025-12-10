"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import type { CreateGeofenceRequest } from "@/types";
import { geofencesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { GeofenceForm } from "@/components/geofences";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { ArrowLeft, MapPin } from "lucide-react";

export default function NewGeofencePage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);

  const { loading, execute } = useApi();

  const handleSubmit = async (data: CreateGeofenceRequest) => {
    setError(null);

    const result = await execute(() => geofencesApi.create(data));
    if (result) {
      router.push("/geofences");
    } else {
      setError("Failed to create geofence. Please try again.");
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Link href="/geofences">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Geofences
          </Button>
        </Link>
      </div>

      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <MapPin className="h-8 w-8" />
          Create Geofence
        </h1>
        <p className="text-muted-foreground">
          Set up a new location-based trigger for a device
        </p>
      </div>

      {/* Form */}
      <Card>
        <CardHeader>
          <CardTitle>Geofence Details</CardTitle>
          <CardDescription>
            Configure the geofence location and trigger settings
          </CardDescription>
        </CardHeader>
        <CardContent>
          {error && (
            <div className="mb-4 p-3 bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 rounded-md">
              {error}
            </div>
          )}
          <GeofenceForm onSubmit={handleSubmit} loading={loading} />
        </CardContent>
      </Card>
    </div>
  );
}
