"use client";

import { AdminGeofenceList } from "@/components/geofences";
import { MapPin } from "lucide-react";

export default function GeofencesPage() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <MapPin className="h-8 w-8" />
          Geofences
        </h1>
        <p className="text-muted-foreground">
          Manage location-based triggers for devices
        </p>
      </div>

      {/* Geofence List */}
      <AdminGeofenceList />
    </div>
  );
}
