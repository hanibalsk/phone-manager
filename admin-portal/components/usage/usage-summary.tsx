"use client";

import type { AppUsage } from "@/types";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Clock, Smartphone, TrendingUp } from "lucide-react";

interface UsageSummaryProps {
  usage: AppUsage[];
}

export function UsageSummary({ usage }: UsageSummaryProps) {
  const totalMinutes = usage.reduce((sum, app) => sum + app.usageTimeMinutes, 0);
  const appCount = usage.length;
  const avgPerApp = appCount > 0 ? Math.round(totalMinutes / appCount) : 0;

  const formatTime = (minutes: number) => {
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  return (
    <div className="grid gap-4 md:grid-cols-3">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Total Screen Time</CardTitle>
          <Clock className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{formatTime(totalMinutes)}</div>
          <p className="text-xs text-muted-foreground">Today</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Apps Used</CardTitle>
          <Smartphone className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{appCount}</div>
          <p className="text-xs text-muted-foreground">Different apps</p>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-sm font-medium">Avg per App</CardTitle>
          <TrendingUp className="h-4 w-4 text-muted-foreground" />
        </CardHeader>
        <CardContent>
          <div className="text-2xl font-bold">{formatTime(avgPerApp)}</div>
          <p className="text-xs text-muted-foreground">Average time</p>
        </CardContent>
      </Card>
    </div>
  );
}
