"use client";

import { useState, useEffect, useCallback } from "react";
import type { ProximityAlertTrigger } from "@/types";
import { proximityAlertsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { RefreshCw, MapPin, Ruler, Clock } from "lucide-react";

const ITEMS_PER_PAGE = 25;

interface AlertTriggerHistoryProps {
  alertId: string;
  alertName: string;
}

export function AlertTriggerHistory({ alertId, alertName }: AlertTriggerHistoryProps) {
  const [page, setPage] = useState(1);

  const { data: triggersData, loading, error, execute: fetchTriggers } = useApi<{
    items: ProximityAlertTrigger[];
    total: number;
  }>();

  const loadTriggers = useCallback(() => {
    fetchTriggers(() =>
      proximityAlertsApi.getTriggers(alertId, {
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchTriggers, alertId, page]);

  useEffect(() => {
    loadTriggers();
  }, [loadTriggers]);

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };

  const formatDistance = (meters: number) => {
    if (meters >= 1000) {
      return `${(meters / 1000).toFixed(2)} km`;
    }
    return `${meters.toFixed(1)} m`;
  };

  const triggers = triggersData?.items || [];
  const total = triggersData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Trigger History</CardTitle>
            <CardDescription>
              {total} trigger{total !== 1 ? "s" : ""} for &quot;{alertName}&quot;
            </CardDescription>
          </div>
          <Button variant="outline" size="sm" onClick={loadTriggers} disabled={loading}>
            <RefreshCw className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`} />
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {/* Error State */}
        {error && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <p className="text-destructive mb-4">{error}</p>
            <Button variant="outline" onClick={loadTriggers}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Retry
            </Button>
          </div>
        )}

        {/* Loading State */}
        {loading && !triggersData && (
          <div className="space-y-3">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-4 py-3">
                <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                <div className="h-4 w-20 bg-muted animate-pulse rounded" />
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && triggers.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Clock className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No triggers recorded yet</p>
            <p className="text-sm text-muted-foreground mt-1">
              Triggers will appear here when devices come within the configured distance
            </p>
          </div>
        )}

        {/* Triggers Table */}
        {!error && triggers.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Time</th>
                    <th className="text-left py-3 px-4 font-medium">Distance</th>
                    <th className="text-left py-3 px-4 font-medium">Device A Location</th>
                    <th className="text-left py-3 px-4 font-medium">Device B Location</th>
                  </tr>
                </thead>
                <tbody>
                  {triggers.map((trigger) => (
                    <tr key={trigger.id} className="border-b hover:bg-muted/50">
                      <td className="py-3 px-4 text-sm">
                        {formatDateTime(trigger.triggered_at)}
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1.5">
                          <Ruler className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium text-orange-600 dark:text-orange-400">
                            {formatDistance(trigger.distance_meters)}
                          </span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm font-mono">
                        <div className="flex items-center gap-1.5">
                          <MapPin className="h-4 w-4 text-blue-500" />
                          {trigger.device_a_latitude.toFixed(6)}, {trigger.device_a_longitude.toFixed(6)}
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm font-mono">
                        <div className="flex items-center gap-1.5">
                          <MapPin className="h-4 w-4 text-green-500" />
                          {trigger.device_b_latitude.toFixed(6)}, {trigger.device_b_longitude.toFixed(6)}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-muted-foreground">
                  Page {page} of {totalPages}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(1, p - 1))}
                    disabled={page === 1}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.min(totalPages, p + 1))}
                    disabled={page === totalPages}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
}
