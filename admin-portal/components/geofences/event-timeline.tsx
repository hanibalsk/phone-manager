"use client";

import { useMemo, useState } from "react";
import type { GeofenceEvent, GeofenceEventType } from "@/types";
import { EventTypeBadge } from "./event-type-badge";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Clock,
  ChevronDown,
  ChevronUp,
  Layers,
  Smartphone,
} from "lucide-react";

interface EventTimelineProps {
  events: GeofenceEvent[];
  className?: string;
}

type GroupBy = "time" | "geofence" | "device";

const EVENT_TYPE_COLORS: Record<GeofenceEventType, string> = {
  ENTER: "bg-green-500",
  EXIT: "bg-red-500",
  DWELL: "bg-blue-500",
};

const EVENT_TYPE_LIGHT_COLORS: Record<GeofenceEventType, string> = {
  ENTER: "bg-green-100 dark:bg-green-900/30 border-green-300 dark:border-green-700",
  EXIT: "bg-red-100 dark:bg-red-900/30 border-red-300 dark:border-red-700",
  DWELL: "bg-blue-100 dark:bg-blue-900/30 border-blue-300 dark:border-blue-700",
};

export function EventTimeline({ events, className = "" }: EventTimelineProps) {
  const [groupBy, setGroupBy] = useState<GroupBy>("time");
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set());

  // Sort events by time
  const sortedEvents = useMemo(() => {
    return [...events].sort(
      (a, b) => new Date(a.triggered_at).getTime() - new Date(b.triggered_at).getTime()
    );
  }, [events]);

  // Group events based on selected grouping
  const groupedEvents = useMemo(() => {
    if (groupBy === "time") {
      // Group by date
      const groups: Record<string, GeofenceEvent[]> = {};
      sortedEvents.forEach((event) => {
        const date = new Date(event.triggered_at).toLocaleDateString("en-US", {
          weekday: "short",
          month: "short",
          day: "numeric",
        });
        if (!groups[date]) groups[date] = [];
        groups[date].push(event);
      });
      return groups;
    } else if (groupBy === "geofence") {
      const groups: Record<string, GeofenceEvent[]> = {};
      sortedEvents.forEach((event) => {
        const key = event.geofence_name;
        if (!groups[key]) groups[key] = [];
        groups[key].push(event);
      });
      return groups;
    } else {
      const groups: Record<string, GeofenceEvent[]> = {};
      sortedEvents.forEach((event) => {
        const key = event.device_name;
        if (!groups[key]) groups[key] = [];
        groups[key].push(event);
      });
      return groups;
    }
  }, [sortedEvents, groupBy]);

  const toggleGroup = (groupKey: string) => {
    setExpandedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(groupKey)) {
        next.delete(groupKey);
      } else {
        next.add(groupKey);
      }
      return next;
    });
  };

  const formatTime = (dateStr: string) => {
    return new Date(dateStr).toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatDwellTime = (seconds: number | null) => {
    if (seconds === null) return null;
    if (seconds < 60) return `${seconds}s`;
    if (seconds < 3600) return `${Math.floor(seconds / 60)}m`;
    const hours = Math.floor(seconds / 3600);
    const mins = Math.floor((seconds % 3600) / 60);
    return `${hours}h ${mins}m`;
  };

  if (events.length === 0) {
    return (
      <div className={`flex flex-col items-center justify-center py-8 text-center ${className}`}>
        <Clock className="h-12 w-12 text-muted-foreground mb-4" />
        <p className="text-muted-foreground">No events to display</p>
        <p className="text-sm text-muted-foreground mt-1">
          Events will appear here when devices enter or exit geofences
        </p>
      </div>
    );
  }

  // Calculate time range for visualization
  const timeRange = useMemo(() => {
    if (sortedEvents.length === 0) return { min: 0, max: 0, span: 0 };
    const min = new Date(sortedEvents[0].triggered_at).getTime();
    const max = new Date(sortedEvents[sortedEvents.length - 1].triggered_at).getTime();
    return { min, max, span: max - min || 1 };
  }, [sortedEvents]);

  return (
    <div className={className}>
      {/* Controls */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <Label className="text-sm">Group by:</Label>
            <div className="flex gap-1">
              <Button
                variant={groupBy === "time" ? "default" : "outline"}
                size="sm"
                onClick={() => setGroupBy("time")}
              >
                <Clock className="h-3 w-3 mr-1" />
                Time
              </Button>
              <Button
                variant={groupBy === "geofence" ? "default" : "outline"}
                size="sm"
                onClick={() => setGroupBy("geofence")}
              >
                <Layers className="h-3 w-3 mr-1" />
                Geofence
              </Button>
              <Button
                variant={groupBy === "device" ? "default" : "outline"}
                size="sm"
                onClick={() => setGroupBy("device")}
              >
                <Smartphone className="h-3 w-3 mr-1" />
                Device
              </Button>
            </div>
          </div>
        </div>

        {/* Legend */}
        <div className="flex items-center gap-3 text-xs">
          <div className="flex items-center gap-1">
            <div className={`w-3 h-3 rounded-full ${EVENT_TYPE_COLORS.ENTER}`} />
            <span>Enter</span>
          </div>
          <div className="flex items-center gap-1">
            <div className={`w-3 h-3 rounded-full ${EVENT_TYPE_COLORS.EXIT}`} />
            <span>Exit</span>
          </div>
          <div className="flex items-center gap-1">
            <div className={`w-3 h-3 rounded-full ${EVENT_TYPE_COLORS.DWELL}`} />
            <span>Dwell</span>
          </div>
        </div>
      </div>

      {/* Timeline Visualization */}
      <div className="space-y-4">
        {Object.entries(groupedEvents).map(([groupKey, groupEvents]) => {
          const isExpanded = expandedGroups.has(groupKey);
          const eventCounts = groupEvents.reduce(
            (acc, e) => {
              acc[e.event_type] = (acc[e.event_type] || 0) + 1;
              return acc;
            },
            {} as Record<string, number>
          );

          return (
            <div key={groupKey} className="border rounded-lg overflow-hidden">
              {/* Group Header */}
              <button
                className="w-full flex items-center justify-between p-3 bg-muted/50 hover:bg-muted transition-colors"
                onClick={() => toggleGroup(groupKey)}
              >
                <div className="flex items-center gap-3">
                  <span className="font-medium">{groupKey}</span>
                  <span className="text-sm text-muted-foreground">
                    {groupEvents.length} event{groupEvents.length !== 1 ? "s" : ""}
                  </span>
                  {/* Mini event type summary */}
                  <div className="flex items-center gap-2">
                    {eventCounts.ENTER && (
                      <span className="text-xs text-green-600 dark:text-green-400">
                        {eventCounts.ENTER} enter
                      </span>
                    )}
                    {eventCounts.EXIT && (
                      <span className="text-xs text-red-600 dark:text-red-400">
                        {eventCounts.EXIT} exit
                      </span>
                    )}
                    {eventCounts.DWELL && (
                      <span className="text-xs text-blue-600 dark:text-blue-400">
                        {eventCounts.DWELL} dwell
                      </span>
                    )}
                  </div>
                </div>
                {isExpanded ? (
                  <ChevronUp className="h-4 w-4 text-muted-foreground" />
                ) : (
                  <ChevronDown className="h-4 w-4 text-muted-foreground" />
                )}
              </button>

              {/* Timeline Bar (always visible) */}
              <div className="px-3 py-2 bg-background border-t">
                <div className="relative h-6 bg-muted rounded-full overflow-hidden">
                  {groupEvents.map((event, idx) => {
                    const eventTime = new Date(event.triggered_at).getTime();
                    const position = ((eventTime - timeRange.min) / timeRange.span) * 100;
                    return (
                      <div
                        key={event.id}
                        className={`absolute top-1 w-4 h-4 rounded-full ${EVENT_TYPE_COLORS[event.event_type]} transform -translate-x-1/2 cursor-pointer hover:scale-125 transition-transform ring-2 ring-background`}
                        style={{ left: `${Math.min(Math.max(position, 2), 98)}%` }}
                        title={`${event.event_type} - ${event.geofence_name} - ${formatTime(event.triggered_at)}`}
                      />
                    );
                  })}
                </div>
                {/* Time labels */}
                {groupEvents.length > 0 && (
                  <div className="flex justify-between text-xs text-muted-foreground mt-1">
                    <span>{formatTime(groupEvents[0].triggered_at)}</span>
                    <span>{formatTime(groupEvents[groupEvents.length - 1].triggered_at)}</span>
                  </div>
                )}
              </div>

              {/* Expanded Event List */}
              {isExpanded && (
                <div className="border-t divide-y">
                  {groupEvents.map((event) => (
                    <div
                      key={event.id}
                      className={`flex items-center gap-4 p-3 ${EVENT_TYPE_LIGHT_COLORS[event.event_type]} border-l-4`}
                      style={{
                        borderLeftColor:
                          event.event_type === "ENTER"
                            ? "rgb(34, 197, 94)"
                            : event.event_type === "EXIT"
                            ? "rgb(239, 68, 68)"
                            : "rgb(59, 130, 246)",
                      }}
                    >
                      <div className="flex-shrink-0">
                        <EventTypeBadge type={event.event_type} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          {groupBy !== "geofence" && (
                            <span className="font-medium text-sm">{event.geofence_name}</span>
                          )}
                          {groupBy !== "device" && (
                            <span className="text-sm text-muted-foreground">
                              {event.device_name}
                            </span>
                          )}
                        </div>
                        <div className="text-xs text-muted-foreground">
                          {event.latitude.toFixed(6)}, {event.longitude.toFixed(6)}
                          {event.dwell_time_seconds && (
                            <span className="ml-2">
                              Dwell: {formatDwellTime(event.dwell_time_seconds)}
                            </span>
                          )}
                        </div>
                      </div>
                      <div className="text-sm text-muted-foreground flex-shrink-0">
                        {formatTime(event.triggered_at)}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
