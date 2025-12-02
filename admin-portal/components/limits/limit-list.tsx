"use client";

import { useState } from "react";
import type { DailyLimit } from "@/types";
import { LimitEditDialog } from "./limit-edit-dialog";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Clock, Plus, Pencil, Trash2, RefreshCw } from "lucide-react";

interface LimitListProps {
  limits: DailyLimit[];
  loading: boolean;
  onRefresh: () => void;
  onUpdate: (id: string, data: Partial<DailyLimit>) => Promise<void>;
  onDelete: (id: string) => Promise<void>;
  onCreate: (data: Omit<DailyLimit, "id">) => Promise<void>;
}

export function LimitList({
  limits,
  loading,
  onRefresh,
  onUpdate,
  onDelete,
  onCreate,
}: LimitListProps) {
  const [editingLimit, setEditingLimit] = useState<DailyLimit | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const formatTime = (minutes: number) => {
    if (minutes < 60) return `${minutes}m`;
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  const handleSave = async (data: Omit<DailyLimit, "id">) => {
    if (editingLimit) {
      await onUpdate(editingLimit.id, data);
      setEditingLimit(null);
    } else {
      await onCreate(data);
      setIsCreating(false);
    }
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>App Limits</CardTitle>
              <CardDescription>
                Configure daily time limits for apps
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={onRefresh}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
                />
                Refresh
              </Button>
              <Button size="sm" onClick={() => setIsCreating(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Add Limit
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : limits.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Clock className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No app limits configured</p>
              <Button
                variant="outline"
                className="mt-4"
                onClick={() => setIsCreating(true)}
              >
                <Plus className="h-4 w-4 mr-2" />
                Add your first limit
              </Button>
            </div>
          ) : (
            <div className="space-y-3">
              {limits.map((limit) => (
                <div
                  key={limit.id}
                  className="flex items-center justify-between p-4 border rounded-lg"
                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-medium">{limit.appName}</span>
                      <Badge variant={limit.enabled ? "success" : "secondary"}>
                        {limit.enabled ? "Active" : "Disabled"}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Clock className="h-4 w-4" />
                      <span>{formatTime(limit.dailyLimitMinutes)} daily</span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {limit.packageName}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => setEditingLimit(limit)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-destructive"
                      onClick={() => onDelete(limit.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {(editingLimit || isCreating) && (
        <LimitEditDialog
          limit={editingLimit}
          onSave={handleSave}
          onCancel={() => {
            setEditingLimit(null);
            setIsCreating(false);
          }}
        />
      )}
    </>
  );
}
