"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
import type { Geofence, CreateGeofenceRequest } from "@/types";
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
import { ArrowLeft, MapPin, RefreshCw } from "lucide-react";

export default function EditGeofencePage() {
  const router = useRouter();
  const params = useParams();
  const geofenceId = params.id as string;

  const [error, setError] = useState<string | null>(null);

  const {
    data: geofence,
    loading: loadingGeofence,
    error: fetchError,
    execute: fetchGeofence,
  } = useApi<Geofence>();
  const { loading: updating, execute: updateGeofence } = useApi<Geofence>();

  useEffect(() => {
    fetchGeofence(() => geofencesApi.get(geofenceId));
  }, [fetchGeofence, geofenceId]);

  const handleSubmit = async (data: CreateGeofenceRequest) => {
    setError(null);

    const result = await updateGeofence(() => geofencesApi.update(geofenceId, data));
    if (result) {
      router.push("/geofences");
    } else {
      setError("Failed to update geofence. Please try again.");
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
          Edit Geofence
        </h1>
        <p className="text-muted-foreground">
          Update geofence configuration and triggers
        </p>
      </div>

      {/* Loading */}
      {loadingGeofence && (
        <Card>
          <CardContent className="py-8">
            <div className="flex flex-col items-center justify-center">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
              <p className="mt-2 text-muted-foreground">Loading geofence...</p>
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
                onClick={() => fetchGeofence(() => geofencesApi.get(geofenceId))}
              >
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Form */}
      {geofence && !loadingGeofence && (
        <Card>
          <CardHeader>
            <CardTitle>Geofence Details</CardTitle>
            <CardDescription>
              Update the geofence configuration for &quot;{geofence.name}&quot;
            </CardDescription>
          </CardHeader>
          <CardContent>
            {error && (
              <div className="mb-4 p-3 bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400 rounded-md">
                {error}
              </div>
            )}
            <GeofenceForm
              geofence={geofence}
              onSubmit={handleSubmit}
              loading={updating}
            />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
