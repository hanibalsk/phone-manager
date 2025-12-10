"use client";

import { useState } from "react";
import type { RetentionPolicy } from "@/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2 } from "lucide-react";

interface RetentionPolicyFormProps {
  policy: RetentionPolicy;
  onSubmit: (data: {
    location_retention_days: number;
    event_retention_days: number;
    trip_retention_days: number;
  }) => Promise<void>;
  onCancel: () => void;
  loading?: boolean;
}

export function RetentionPolicyForm({
  policy,
  onSubmit,
  onCancel,
  loading,
}: RetentionPolicyFormProps) {
  const [locationDays, setLocationDays] = useState(policy.location_retention_days.toString());
  const [eventDays, setEventDays] = useState(policy.event_retention_days.toString());
  const [tripDays, setTripDays] = useState(policy.trip_retention_days.toString());

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    await onSubmit({
      location_retention_days: parseInt(locationDays, 10),
      event_retention_days: parseInt(eventDays, 10),
      trip_retention_days: parseInt(tripDays, 10),
    });
  };

  const isValid =
    parseInt(locationDays, 10) > 0 &&
    parseInt(eventDays, 10) > 0 &&
    parseInt(tripDays, 10) > 0;

  // Common presets
  const applyPreset = (days: number) => {
    setLocationDays(days.toString());
    setEventDays(days.toString());
    setTripDays(days.toString());
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="space-y-2">
        <Label htmlFor="locationDays">Location Data Retention (days)</Label>
        <Input
          id="locationDays"
          type="number"
          min="1"
          max="3650"
          value={locationDays}
          onChange={(e) => setLocationDays(e.target.value)}
          required
        />
        <p className="text-xs text-muted-foreground">
          GPS coordinates and location history
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="eventDays">Event Data Retention (days)</Label>
        <Input
          id="eventDays"
          type="number"
          min="1"
          max="3650"
          value={eventDays}
          onChange={(e) => setEventDays(e.target.value)}
          required
        />
        <p className="text-xs text-muted-foreground">
          Geofence events and proximity alerts
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="tripDays">Trip Data Retention (days)</Label>
        <Input
          id="tripDays"
          type="number"
          min="1"
          max="3650"
          value={tripDays}
          onChange={(e) => setTripDays(e.target.value)}
          required
        />
        <p className="text-xs text-muted-foreground">
          Trip records and route history
        </p>
      </div>

      {/* Quick Presets */}
      <div className="space-y-2">
        <Label>Quick Presets</Label>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => applyPreset(30)}
          >
            30 days
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => applyPreset(90)}
          >
            90 days
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => applyPreset(180)}
          >
            6 months
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => applyPreset(365)}
          >
            1 year
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => applyPreset(730)}
          >
            2 years
          </Button>
        </div>
      </div>

      <div className="flex justify-end gap-2 pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button type="submit" disabled={!isValid || loading}>
          {loading ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Saving...
            </>
          ) : (
            "Save Changes"
          )}
        </Button>
      </div>
    </form>
  );
}
