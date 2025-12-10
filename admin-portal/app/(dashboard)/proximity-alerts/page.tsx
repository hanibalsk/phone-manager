"use client";

import { AdminProximityAlertList } from "@/components/proximity";
import { Users } from "lucide-react";

export default function ProximityAlertsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <Users className="h-8 w-8" />
          Proximity Alerts
        </h1>
        <p className="text-muted-foreground">
          Monitor distance between device pairs and receive alerts when they come close
        </p>
      </div>

      <AdminProximityAlertList />
    </div>
  );
}
