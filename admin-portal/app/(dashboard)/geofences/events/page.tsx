"use client";

import { GeofenceEventsList } from "@/components/geofences";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { ArrowLeft, Activity } from "lucide-react";

export default function GeofenceEventsPage() {
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
          <Activity className="h-8 w-8" />
          Geofence Events
        </h1>
        <p className="text-muted-foreground">
          Monitor device entry, exit, and dwell events across all geofences
        </p>
      </div>

      {/* Events List */}
      <GeofenceEventsList />
    </div>
  );
}
