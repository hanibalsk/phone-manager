"use client";

import type { LatestDeviceLocation, AdminDeviceStatus } from "@/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import {
  Smartphone,
  Battery,
  Clock,
  MapPin,
  ExternalLink,
} from "lucide-react";

interface DeviceMarkerPopupProps {
  device: LatestDeviceLocation;
  onClose: () => void;
}

const statusConfig: Record<AdminDeviceStatus, { label: string; className: string }> = {
  active: {
    label: "Active",
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  suspended: {
    label: "Suspended",
    className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  },
  offline: {
    label: "Offline",
    className: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
  },
  pending: {
    label: "Pending",
    className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  },
};

export function DeviceMarkerPopup({ device, onClose }: DeviceMarkerPopupProps) {
  const config = statusConfig[device.status];

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatBattery = (level: number | null) => {
    if (level === null) return "Unknown";
    return `${Math.round(level * 100)}%`;
  };

  return (
    <div className="bg-background border rounded-lg shadow-lg p-4 min-w-[280px]">
      <div className="flex items-start justify-between mb-3">
        <div className="flex items-center gap-2">
          <Smartphone className="h-5 w-5 text-muted-foreground" />
          <div>
            <h3 className="font-semibold">{device.device_name}</h3>
            <p className="text-xs text-muted-foreground">
              {device.organization_name}
            </p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="text-muted-foreground hover:text-foreground"
          aria-label="Close"
        >
          &times;
        </button>
      </div>

      <div className="space-y-2 mb-3">
        <div className="flex items-center justify-between">
          <span className="text-sm text-muted-foreground">Status</span>
          <Badge variant="outline" className={config.className}>
            {config.label}
          </Badge>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-sm text-muted-foreground flex items-center gap-1">
            <Battery className="h-3 w-3" />
            Battery
          </span>
          <span className="text-sm">{formatBattery(device.battery_level)}</span>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-sm text-muted-foreground flex items-center gap-1">
            <MapPin className="h-3 w-3" />
            Accuracy
          </span>
          <span className="text-sm">{Math.round(device.accuracy)}m</span>
        </div>

        <div className="flex items-center justify-between">
          <span className="text-sm text-muted-foreground flex items-center gap-1">
            <Clock className="h-3 w-3" />
            Last Update
          </span>
          <span className="text-sm">{formatDateTime(device.timestamp)}</span>
        </div>
      </div>

      <div className="text-xs text-muted-foreground mb-3">
        {device.latitude.toFixed(6)}, {device.longitude.toFixed(6)}
      </div>

      <Link href={`/devices/${device.device_id}`}>
        <Button variant="outline" size="sm" className="w-full">
          <ExternalLink className="h-3 w-3 mr-2" />
          View Device Details
        </Button>
      </Link>
    </div>
  );
}
