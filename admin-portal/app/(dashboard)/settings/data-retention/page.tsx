"use client";

import { RetentionPolicyList } from "@/components/settings";
import { HardDrive } from "lucide-react";

export default function DataRetentionPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <HardDrive className="h-8 w-8" />
          Data Retention
        </h1>
        <p className="text-muted-foreground">
          Configure data retention policies and manage storage for each organization
        </p>
      </div>

      <RetentionPolicyList />
    </div>
  );
}
