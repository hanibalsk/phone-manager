"use client";

import type { AppUsage } from "@/types";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

interface UsageChartProps {
  usage: AppUsage[];
  title?: string;
}

export function UsageChart({ usage, title = "App Usage" }: UsageChartProps) {
  const sortedUsage = [...usage].sort(
    (a, b) => b.usage_time_minutes - a.usage_time_minutes
  );
  const maxUsage = sortedUsage[0]?.usage_time_minutes || 1;

  const formatTime = (minutes: number) => {
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        <CardDescription>
          Time spent in apps today ({sortedUsage.length} apps)
        </CardDescription>
      </CardHeader>
      <CardContent>
        {sortedUsage.length === 0 ? (
          <p className="text-center text-muted-foreground py-8">
            No usage data available
          </p>
        ) : (
          <div className="space-y-3">
            {sortedUsage.slice(0, 10).map((app) => (
              <div key={app.package_name} className="space-y-1">
                <div className="flex justify-between text-sm">
                  <span className="font-medium truncate max-w-[200px]">
                    {app.app_name}
                  </span>
                  <span className="text-muted-foreground">
                    {formatTime(app.usage_time_minutes)}
                  </span>
                </div>
                <div className="h-2 bg-muted rounded-full overflow-hidden">
                  <div
                    className="h-full bg-primary rounded-full transition-all"
                    style={{
                      width: `${(app.usage_time_minutes / maxUsage) * 100}%`,
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
