"use client";

import { useState, useEffect, useCallback } from "react";
import { useParams } from "next/navigation";
import type { Trip, TripPoint, TripEvent } from "@/types";
import { tripsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { TripStatusBadge } from "@/components/trips/trip-status-badge";
import { TripMap } from "@/components/trips/trip-map";
import { TripTimeline } from "@/components/trips/trip-timeline";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import {
  ArrowLeft,
  RefreshCw,
  Clock,
  Gauge,
  MapPin,
  Building2,
  Smartphone,
  Calendar,
} from "lucide-react";

export default function TripDetailPage() {
  const params = useParams();
  const tripId = params.id as string;
  const [activeTab, setActiveTab] = useState<"map" | "timeline">("map");

  const { data: trip, loading: tripLoading, error: tripError, execute: fetchTrip } = useApi<Trip>();
  const { data: pathData, loading: pathLoading, execute: fetchPath } = useApi<{ points: TripPoint[] }>();
  const { data: eventsData, loading: eventsLoading, execute: fetchEvents } = useApi<{ events: TripEvent[] }>();

  const loadTrip = useCallback(() => {
    fetchTrip(() => tripsApi.get(tripId));
    fetchPath(() => tripsApi.getPath(tripId));
    fetchEvents(() => tripsApi.getEvents(tripId));
  }, [fetchTrip, fetchPath, fetchEvents, tripId]);

  useEffect(() => {
    loadTrip();
  }, [loadTrip]);

  const formatDuration = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    if (hours > 0) {
      return `${hours}h ${minutes}m`;
    }
    return `${minutes}m`;
  };

  const formatDistance = (meters: number) => {
    if (meters >= 1000) {
      return `${(meters / 1000).toFixed(2)} km`;
    }
    return `${meters} m`;
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const loading = tripLoading || pathLoading || eventsLoading;

  if (tripLoading && !trip) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Link href="/trips">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
          </Link>
        </div>
        <Card>
          <CardContent className="py-8">
            <div className="flex items-center justify-center">
              <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
              <span className="ml-2 text-muted-foreground">Loading trip...</span>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (tripError || !trip) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Link href="/trips">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
          </Link>
        </div>
        <Card>
          <CardContent className="py-8">
            <div className="flex flex-col items-center justify-center text-center">
              <p className="text-destructive mb-4">{tripError || "Trip not found"}</p>
              <Button variant="outline" onClick={loadTrip}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const points = pathData?.points || [];
  const events = eventsData?.events || [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Link href="/trips">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back
            </Button>
          </Link>
          <div>
            <h1 className="text-2xl font-bold tracking-tight">Trip Details</h1>
            <p className="text-sm text-muted-foreground">
              {trip.device_name} • {formatDate(trip.start_time)}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <TripStatusBadge status={trip.status} />
          <Button variant="outline" size="sm" onClick={loadTrip} disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Duration</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatDuration(trip.duration_seconds)}</div>
            <p className="text-xs text-muted-foreground">
              {trip.end_time ? `Ended ${formatDate(trip.end_time)}` : "In progress"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Distance</CardTitle>
            <Gauge className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatDistance(trip.distance_meters)}</div>
            <p className="text-xs text-muted-foreground">
              {trip.point_count} location points
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Device</CardTitle>
            <Smartphone className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-lg font-bold truncate">{trip.device_name}</div>
            <p className="text-xs text-muted-foreground">
              ID: {trip.device_id.slice(0, 8)}...
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Organization</CardTitle>
            <Building2 className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-lg font-bold truncate">{trip.organization_name}</div>
            <p className="text-xs text-muted-foreground">
              ID: {trip.organization_id.slice(0, 8)}...
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Map and Timeline */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Trip Data</CardTitle>
              <CardDescription>
                View the trip path and movement events
              </CardDescription>
            </div>
            <div className="flex gap-1">
              <Button
                variant={activeTab === "map" ? "default" : "outline"}
                size="sm"
                onClick={() => setActiveTab("map")}
              >
                <MapPin className="h-4 w-4 mr-2" />
                Map
              </Button>
              <Button
                variant={activeTab === "timeline" ? "default" : "outline"}
                size="sm"
                onClick={() => setActiveTab("timeline")}
              >
                <Calendar className="h-4 w-4 mr-2" />
                Timeline
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {activeTab === "map" && (
            <>
              {pathLoading ? (
                <div className="flex items-center justify-center py-12">
                  <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
                  <span className="ml-2 text-muted-foreground">Loading path...</span>
                </div>
              ) : (
                <TripMap points={points} />
              )}
            </>
          )}

          {activeTab === "timeline" && (
            <>
              {eventsLoading ? (
                <div className="flex items-center justify-center py-12">
                  <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
                  <span className="ml-2 text-muted-foreground">Loading events...</span>
                </div>
              ) : (
                <TripTimeline events={events} />
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Location Details */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Start Location</CardTitle>
          </CardHeader>
          <CardContent>
            {trip.start_latitude && trip.start_longitude ? (
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-green-500" />
                  <span className="text-sm font-mono">
                    {trip.start_latitude.toFixed(6)}, {trip.start_longitude.toFixed(6)}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground">
                  {formatDate(trip.start_time)}
                </p>
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">No start location data</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="text-base">End Location</CardTitle>
          </CardHeader>
          <CardContent>
            {trip.end_latitude && trip.end_longitude ? (
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <MapPin className="h-4 w-4 text-red-500" />
                  <span className="text-sm font-mono">
                    {trip.end_latitude.toFixed(6)}, {trip.end_longitude.toFixed(6)}
                  </span>
                </div>
                {trip.end_time && (
                  <p className="text-xs text-muted-foreground">
                    {formatDate(trip.end_time)}
                  </p>
                )}
              </div>
            ) : (
              <p className="text-sm text-muted-foreground">
                {trip.status === "in_progress" ? "Trip still in progress" : "No end location data"}
              </p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
