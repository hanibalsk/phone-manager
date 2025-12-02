"use client";

import type { Device } from "@/types";
import { DeviceStatusBadge } from "./device-status-badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { X, Clock, BarChart3, Settings } from "lucide-react";
import Link from "next/link";

interface DeviceDetailsProps {
  device: Device;
  onClose: () => void;
}

export function DeviceDetails({ device, onClose }: DeviceDetailsProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <Card className="w-full max-w-lg mx-4">
        <CardHeader className="relative">
          <Button
            variant="ghost"
            size="icon"
            className="absolute right-4 top-4"
            onClick={onClose}
          >
            <X className="h-4 w-4" />
          </Button>
          <CardTitle className="flex items-center gap-2">
            {device.name}
            <DeviceStatusBadge lastSeen={device.lastSeen} />
          </CardTitle>
          <CardDescription>Device Details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Android ID
                </p>
                <code className="text-sm bg-muted px-2 py-1 rounded block mt-1">
                  {device.androidId}
                </code>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Status
                </p>
                <p className="text-sm mt-1 capitalize">{device.status}</p>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Enrolled
                </p>
                <p className="text-sm mt-1">{formatDate(device.enrolledAt)}</p>
              </div>
              <div>
                <p className="text-sm font-medium text-muted-foreground">
                  Last Seen
                </p>
                <p className="text-sm mt-1">{formatDate(device.lastSeen)}</p>
              </div>
            </div>
          </div>
        </CardContent>
        <CardFooter className="flex justify-between gap-2">
          <Button variant="outline" asChild>
            <Link href={`/devices/${device.id}/usage`}>
              <BarChart3 className="h-4 w-4 mr-2" />
              View Usage
            </Link>
          </Button>
          <Button variant="outline" asChild>
            <Link href={`/devices/${device.id}/limits`}>
              <Clock className="h-4 w-4 mr-2" />
              Set Limits
            </Link>
          </Button>
          <Button variant="outline" asChild>
            <Link href={`/devices/${device.id}/settings`}>
              <Settings className="h-4 w-4 mr-2" />
              Settings
            </Link>
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
