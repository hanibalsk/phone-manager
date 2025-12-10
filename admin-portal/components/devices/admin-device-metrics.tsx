"use client";

import type { DeviceDetails } from "@/types";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { MapPin, Route, Navigation } from "lucide-react";

interface AdminDeviceMetricsProps {
  device: DeviceDetails;
}

export function AdminDeviceMetrics({ device }: AdminDeviceMetricsProps) {
  const formatCoordinate = (value: number) => {
    return value.toFixed(6);
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <MapPin className="h-5 w-5" />
          Device Metrics
        </CardTitle>
        <CardDescription>Location and tracking statistics</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div className="p-4 rounded-lg bg-muted/50">
            <div className="flex items-center gap-2 text-muted-foreground mb-1">
              <MapPin className="h-4 w-4" />
              <span className="text-sm font-medium">Location Count</span>
            </div>
            <p className="text-2xl font-bold">{device.location_count.toLocaleString()}</p>
          </div>
          <div className="p-4 rounded-lg bg-muted/50">
            <div className="flex items-center gap-2 text-muted-foreground mb-1">
              <Route className="h-4 w-4" />
              <span className="text-sm font-medium">Trip Count</span>
            </div>
            <p className="text-2xl font-bold">{device.trip_count.toLocaleString()}</p>
          </div>
        </div>

        {device.last_location && (
          <div className="border-t pt-4">
            <h4 className="text-sm font-medium mb-3 flex items-center gap-2">
              <Navigation className="h-4 w-4" />
              Last Known Location
            </h4>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  Latitude
                </label>
                <p className="text-sm font-mono">
                  {formatCoordinate(device.last_location.latitude)}
                </p>
              </div>
              <div>
                <label className="text-sm font-medium text-muted-foreground">
                  Longitude
                </label>
                <p className="text-sm font-mono">
                  {formatCoordinate(device.last_location.longitude)}
                </p>
              </div>
              <div className="col-span-2">
                <label className="text-sm font-medium text-muted-foreground">
                  Location Timestamp
                </label>
                <p className="text-sm">{formatDateTime(device.last_location.timestamp)}</p>
              </div>
            </div>
            <div className="mt-3">
              <a
                href={`https://www.google.com/maps?q=${device.last_location.latitude},${device.last_location.longitude}`}
                target="_blank"
                rel="noopener noreferrer"
                className="text-sm text-primary hover:underline"
              >
                View on Google Maps →
              </a>
            </div>
          </div>
        )}

        {!device.last_location && (
          <div className="border-t pt-4 text-center py-6">
            <Navigation className="h-8 w-8 text-muted-foreground mx-auto mb-2" />
            <p className="text-sm text-muted-foreground">
              No location data available
            </p>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
