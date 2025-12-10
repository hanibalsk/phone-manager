"use client";

import type { TripEvent } from "@/types";
import {
  Play,
  Pause,
  Square,
  MapPin,
  Clock,
} from "lucide-react";

interface TripTimelineProps {
  events: TripEvent[];
}

const eventConfig: Record<
  string,
  { label: string; icon: typeof Play; className: string }
> = {
  trip_start: {
    label: "Trip Started",
    icon: Play,
    className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
  },
  stop_detected: {
    label: "Stop Detected",
    icon: Pause,
    className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
  },
  resumed: {
    label: "Trip Resumed",
    icon: Play,
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  trip_end: {
    label: "Trip Ended",
    icon: Square,
    className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
  },
};

export function TripTimeline({ events }: TripTimelineProps) {
  if (events.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <Clock className="h-12 w-12 text-muted-foreground mb-4" />
        <p className="text-muted-foreground">No events recorded</p>
      </div>
    );
  }

  const formatTime = (timestamp: string) => {
    return new Date(timestamp).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const formatDate = (timestamp: string) => {
    return new Date(timestamp).toLocaleDateString([], {
      month: "short",
      day: "numeric",
    });
  };

  // Group events by date
  const eventsByDate = events.reduce(
    (acc, event) => {
      const date = formatDate(event.timestamp);
      if (!acc[date]) {
        acc[date] = [];
      }
      acc[date].push(event);
      return acc;
    },
    {} as Record<string, TripEvent[]>
  );

  return (
    <div className="space-y-6">
      {Object.entries(eventsByDate).map(([date, dateEvents]) => (
        <div key={date}>
          <div className="text-sm font-medium text-muted-foreground mb-3">
            {date}
          </div>
          <div className="relative">
            {/* Timeline line */}
            <div className="absolute left-4 top-0 bottom-0 w-0.5 bg-border" />

            <div className="space-y-4">
              {dateEvents.map((event, index) => {
                const config = eventConfig[event.event_type] || {
                  label: event.event_type.replace(/_/g, " "),
                  icon: MapPin,
                  className: "bg-muted text-muted-foreground",
                };
                const Icon = config.icon;

                return (
                  <div key={event.id} className="relative flex gap-4 pl-10">
                    {/* Timeline dot */}
                    <div
                      className={`absolute left-2 w-5 h-5 rounded-full flex items-center justify-center ${config.className}`}
                    >
                      <Icon className="h-3 w-3" />
                    </div>

                    {/* Event content */}
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between gap-2">
                        <span className="font-medium text-sm">
                          {config.label}
                        </span>
                        <span className="text-xs text-muted-foreground">
                          {formatTime(event.timestamp)}
                        </span>
                      </div>

                      {/* Location */}
                      {event.latitude && event.longitude && (
                        <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
                          <MapPin className="h-3 w-3" />
                          <span>
                            {event.latitude.toFixed(6)}, {event.longitude.toFixed(6)}
                          </span>
                        </div>
                      )}

                      {/* Metadata */}
                      {event.metadata && Object.keys(event.metadata).length > 0 && (
                        <div className="mt-2 p-2 bg-muted/50 rounded text-xs">
                          {Object.entries(event.metadata).map(([key, value]) => (
                            <div key={key} className="flex justify-between">
                              <span className="text-muted-foreground">
                                {key.replace(/_/g, " ")}:
                              </span>
                              <span>{String(value)}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
