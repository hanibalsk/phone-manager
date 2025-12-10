"use client";

import { useState } from "react";
import type { AppUsageByCategory, AppCategory } from "@/types";
import { Button } from "@/components/ui/button";

interface UsageChartProps {
  data: AppUsageByCategory[];
}

const categoryColors: Record<AppCategory, string> = {
  social: "bg-pink-500",
  games: "bg-purple-500",
  productivity: "bg-blue-500",
  entertainment: "bg-orange-500",
  education: "bg-green-500",
  communication: "bg-cyan-500",
  other: "bg-gray-500",
};

const categoryLabels: Record<AppCategory, string> = {
  social: "Social",
  games: "Games",
  productivity: "Productivity",
  entertainment: "Entertainment",
  education: "Education",
  communication: "Communication",
  other: "Other",
};

export function UsageChart({ data }: UsageChartProps) {
  const [viewMode, setViewMode] = useState<"bar" | "breakdown">("bar");

  const maxMinutes = Math.max(...data.map((d) => d.total_minutes), 1);
  const totalMinutes = data.reduce((sum, d) => sum + d.total_minutes, 0);

  const formatTime = (minutes: number) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  return (
    <div className="space-y-4">
      <div className="flex justify-end gap-2">
        <Button
          variant={viewMode === "bar" ? "default" : "outline"}
          size="sm"
          onClick={() => setViewMode("bar")}
        >
          Bar Chart
        </Button>
        <Button
          variant={viewMode === "breakdown" ? "default" : "outline"}
          size="sm"
          onClick={() => setViewMode("breakdown")}
        >
          Breakdown
        </Button>
      </div>

      {viewMode === "bar" ? (
        <div className="space-y-3">
          {data.map((item) => (
            <div key={item.category} className="space-y-1">
              <div className="flex justify-between text-sm">
                <span className="font-medium">{categoryLabels[item.category]}</span>
                <span className="text-muted-foreground">
                  {formatTime(item.total_minutes)} ({item.percentage.toFixed(1)}%)
                </span>
              </div>
              <div className="h-4 w-full rounded-full bg-secondary overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-300 ${categoryColors[item.category]}`}
                  style={{ width: `${(item.total_minutes / maxMinutes) * 100}%` }}
                />
              </div>
              <div className="text-xs text-muted-foreground">
                {item.app_count} app{item.app_count !== 1 ? "s" : ""}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="space-y-4">
          {/* Stacked bar */}
          <div className="h-8 w-full rounded-lg overflow-hidden flex">
            {data.map((item) => (
              <div
                key={item.category}
                className={`h-full ${categoryColors[item.category]} transition-all duration-300`}
                style={{ width: `${item.percentage}%` }}
                title={`${categoryLabels[item.category]}: ${formatTime(item.total_minutes)}`}
              />
            ))}
          </div>

          {/* Legend */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
            {data.map((item) => (
              <div key={item.category} className="flex items-center gap-2 text-sm">
                <div className={`h-3 w-3 rounded ${categoryColors[item.category]}`} />
                <span className="text-muted-foreground">{categoryLabels[item.category]}</span>
                <span className="font-medium">{item.percentage.toFixed(0)}%</span>
              </div>
            ))}
          </div>

          {/* Total */}
          <div className="text-center text-sm text-muted-foreground">
            Total: <span className="font-medium text-foreground">{formatTime(totalMinutes)}</span>
          </div>
        </div>
      )}
    </div>
  );
}
