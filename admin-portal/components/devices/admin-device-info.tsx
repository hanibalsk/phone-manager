"use client";

import type { DeviceDetails } from "@/types";
import { DevicePlatformBadge } from "./device-platform-badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Smartphone, User, Building2 } from "lucide-react";

interface AdminDeviceInfoProps {
  device: DeviceDetails;
}

export function AdminDeviceInfo({ device }: AdminDeviceInfoProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Smartphone className="h-5 w-5" />
          Device Information
        </CardTitle>
        <CardDescription>Basic device details and identifiers</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="text-sm font-medium text-muted-foreground">
              Device Name
            </label>
            <p className="text-sm font-medium">{device.display_name}</p>
          </div>
          <div>
            <label className="text-sm font-medium text-muted-foreground">
              Platform
            </label>
            <div className="mt-1">
              <DevicePlatformBadge platform={device.platform} />
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-muted-foreground">
              Device UUID
            </label>
            <p className="text-sm font-mono bg-muted px-2 py-1 rounded">
              {device.device_id}
            </p>
          </div>
          <div>
            <label className="text-sm font-medium text-muted-foreground">
              Created At
            </label>
            <p className="text-sm">
              {new Date(device.created_at).toLocaleDateString("en-US", {
                year: "numeric",
                month: "long",
                day: "numeric",
              })}
            </p>
          </div>
        </div>

        <div className="border-t pt-4">
          <h4 className="text-sm font-medium mb-3 flex items-center gap-2">
            <User className="h-4 w-4" />
            Owner Information
          </h4>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-muted-foreground">
                Owner Email
              </label>
              <p className="text-sm">{device.owner_email}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-muted-foreground">
                Owner ID
              </label>
              <p className="text-sm font-mono text-xs bg-muted px-2 py-1 rounded">
                {device.owner_id}
              </p>
            </div>
          </div>
        </div>

        <div className="border-t pt-4">
          <h4 className="text-sm font-medium mb-3 flex items-center gap-2">
            <Building2 className="h-4 w-4" />
            Organization
          </h4>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-muted-foreground">
                Organization
              </label>
              <p className="text-sm">{device.organization_name}</p>
            </div>
            <div>
              <label className="text-sm font-medium text-muted-foreground">
                Group
              </label>
              <p className="text-sm">{device.group_name || "—"}</p>
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
