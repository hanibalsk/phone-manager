"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import type { AutoApprovalRule, Organization } from "@/types";
import { unlockRequestsApi, organizationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Zap,
  Plus,
  RefreshCw,
  ChevronUp,
  ChevronDown,
  Pencil,
  Trash2,
  Clock,
  User,
  Smartphone,
  Users,
  ToggleLeft,
  ToggleRight,
} from "lucide-react";

export function AutoApprovalRules() {
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const { data: rulesData, loading, error, execute: fetchRules } = useApi<{ items: AutoApprovalRule[] }>();
  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { execute: updateRule } = useApi<AutoApprovalRule>();
  const { execute: deleteRule } = useApi<void>();
  const { execute: reorderRules } = useApi<void>();

  useEffect(() => {
    fetchRules(() => unlockRequestsApi.listAutoApprovalRules());
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchRules, fetchOrgs]);

  // Clear notification after 3 seconds
  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  const handleToggle = useCallback(async (rule: AutoApprovalRule) => {
    const result = await updateRule(() =>
      unlockRequestsApi.updateAutoApprovalRule(rule.id, { enabled: !rule.enabled })
    );
    if (result) {
      setNotification({ type: "success", message: `Rule ${rule.enabled ? "disabled" : "enabled"}` });
      fetchRules(() => unlockRequestsApi.listAutoApprovalRules());
    } else {
      setNotification({ type: "error", message: "Failed to update rule" });
    }
  }, [updateRule, fetchRules]);

  const handleDelete = useCallback(async (ruleId: string) => {
    if (!confirm("Are you sure you want to delete this rule?")) return;

    const result = await deleteRule(() =>
      unlockRequestsApi.deleteAutoApprovalRule(ruleId)
    );
    if (result !== undefined) {
      setNotification({ type: "success", message: "Rule deleted" });
      fetchRules(() => unlockRequestsApi.listAutoApprovalRules());
    } else {
      setNotification({ type: "error", message: "Failed to delete rule" });
    }
  }, [deleteRule, fetchRules]);

  const handleReorder = useCallback(async (ruleId: string, direction: "up" | "down") => {
    const rules = rulesData?.items || [];
    const currentIndex = rules.findIndex(r => r.id === ruleId);
    if (currentIndex === -1) return;

    const newIndex = direction === "up" ? currentIndex - 1 : currentIndex + 1;
    if (newIndex < 0 || newIndex >= rules.length) return;

    const newOrder = [...rules];
    [newOrder[currentIndex], newOrder[newIndex]] = [newOrder[newIndex], newOrder[currentIndex]];
    const ruleIds = newOrder.map(r => r.id);

    const result = await reorderRules(() =>
      unlockRequestsApi.reorderAutoApprovalRules({ rule_ids: ruleIds })
    );
    if (result !== undefined) {
      fetchRules(() => unlockRequestsApi.listAutoApprovalRules());
    } else {
      setNotification({ type: "error", message: "Failed to reorder rules" });
    }
  }, [rulesData, reorderRules, fetchRules]);

  const getOrgName = (orgId: string) => {
    return orgsData?.items?.find(o => o.id === orgId)?.name || orgId;
  };

  const formatConditions = (rule: AutoApprovalRule) => {
    const conditions: string[] = [];

    if (rule.conditions.time_window) {
      const tw = rule.conditions.time_window;
      conditions.push(`${tw.start_time}-${tw.end_time} (${tw.days_of_week?.map((d: number) => ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"][d]).join(", ")})`);
    }
    if (rule.conditions.user_ids?.length) {
      conditions.push(`${rule.conditions.user_ids.length} user(s)`);
    }
    if (rule.conditions.device_ids?.length) {
      conditions.push(`${rule.conditions.device_ids.length} device(s)`);
    }
    if (rule.conditions.group_ids?.length) {
      conditions.push(`${rule.conditions.group_ids.length} group(s)`);
    }
    if (rule.conditions.max_daily_requests) {
      conditions.push(`Max ${rule.conditions.max_daily_requests}/day`);
    }

    return conditions;
  };

  const formatDuration = (minutes: number) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  const rules = rulesData?.items || [];

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Zap className="h-6 w-6" />
            <div>
              <CardTitle>Auto-Approval Rules</CardTitle>
              <CardDescription>
                Configure automatic approval for unlock requests
              </CardDescription>
            </div>
          </div>
          <div className="flex gap-2">
            <Link href="/unlock-requests/rules/new">
              <Button size="sm">
                <Plus className="mr-2 h-4 w-4" />
                Add Rule
              </Button>
            </Link>
            <Button variant="outline" size="sm" onClick={() => fetchRules(() => unlockRequestsApi.listAutoApprovalRules())}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Notification */}
        {notification && (
          <div
            className={`mb-4 p-3 rounded-md ${
              notification.type === "success"
                ? "bg-green-100 text-green-800"
                : "bg-red-100 text-red-800"
            }`}
          >
            {notification.message}
          </div>
        )}

        {/* Rules prioritization info */}
        <div className="mb-4 p-3 bg-secondary/50 rounded-lg text-sm text-muted-foreground">
          <strong>Priority:</strong> Rules are evaluated from top to bottom. The first matching rule will be applied.
          Use the arrows to change priority order.
        </div>

        {/* Rules List */}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="text-muted-foreground">Loading rules...</div>
          </div>
        ) : error ? (
          <div className="flex justify-center py-8 text-red-500">
            Error loading rules
          </div>
        ) : rules.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <Zap className="h-12 w-12 mb-4 opacity-50" />
            <p>No auto-approval rules configured</p>
            <Link href="/unlock-requests/rules/new" className="mt-4">
              <Button>Create Your First Rule</Button>
            </Link>
          </div>
        ) : (
          <div className="space-y-3">
            {rules.map((rule, index) => (
              <div
                key={rule.id}
                className={`border rounded-lg p-4 ${!rule.enabled ? "opacity-60 bg-secondary/30" : ""}`}
              >
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      <span className="text-sm font-medium bg-primary/10 text-primary px-2 py-0.5 rounded">
                        #{index + 1}
                      </span>
                      <span className="font-medium">{rule.name}</span>
                      {!rule.enabled && (
                        <span className="text-xs text-muted-foreground bg-secondary px-2 py-0.5 rounded">
                          Disabled
                        </span>
                      )}
                    </div>

                    {rule.description && (
                      <p className="text-sm text-muted-foreground mb-2">{rule.description}</p>
                    )}

                    <div className="flex flex-wrap gap-2 text-sm">
                      <span className="text-muted-foreground">
                        Org: {getOrgName(rule.organization_id)}
                      </span>
                      <span className="text-muted-foreground">|</span>
                      <span className="flex items-center gap-1 text-green-600">
                        <Clock className="h-3 w-3" />
                        Max: {formatDuration(rule.max_duration_minutes)}
                      </span>
                    </div>

                    {/* Conditions */}
                    <div className="mt-2 flex flex-wrap gap-2">
                      {rule.conditions.time_window && (
                        <span className="inline-flex items-center gap-1 text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded">
                          <Clock className="h-3 w-3" />
                          {rule.conditions.time_window.start_time}-{rule.conditions.time_window.end_time}
                        </span>
                      )}
                      {rule.conditions.user_ids?.length && (
                        <span className="inline-flex items-center gap-1 text-xs bg-purple-100 text-purple-700 px-2 py-1 rounded">
                          <User className="h-3 w-3" />
                          {rule.conditions.user_ids.length} user(s)
                        </span>
                      )}
                      {rule.conditions.device_ids?.length && (
                        <span className="inline-flex items-center gap-1 text-xs bg-orange-100 text-orange-700 px-2 py-1 rounded">
                          <Smartphone className="h-3 w-3" />
                          {rule.conditions.device_ids.length} device(s)
                        </span>
                      )}
                      {rule.conditions.group_ids?.length && (
                        <span className="inline-flex items-center gap-1 text-xs bg-green-100 text-green-700 px-2 py-1 rounded">
                          <Users className="h-3 w-3" />
                          {rule.conditions.group_ids.length} group(s)
                        </span>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-2 ml-4">
                    {/* Reorder buttons */}
                    <div className="flex flex-col">
                      <button
                        className="p-1 hover:bg-secondary rounded disabled:opacity-30"
                        disabled={index === 0}
                        onClick={() => handleReorder(rule.id, "up")}
                        title="Move up"
                      >
                        <ChevronUp className="h-4 w-4" />
                      </button>
                      <button
                        className="p-1 hover:bg-secondary rounded disabled:opacity-30"
                        disabled={index === rules.length - 1}
                        onClick={() => handleReorder(rule.id, "down")}
                        title="Move down"
                      >
                        <ChevronDown className="h-4 w-4" />
                      </button>
                    </div>

                    {/* Toggle */}
                    <button
                      className="p-2 hover:bg-secondary rounded"
                      onClick={() => handleToggle(rule)}
                      title={rule.enabled ? "Disable" : "Enable"}
                    >
                      {rule.enabled ? (
                        <ToggleRight className="h-5 w-5 text-green-600" />
                      ) : (
                        <ToggleLeft className="h-5 w-5 text-muted-foreground" />
                      )}
                    </button>

                    {/* Edit */}
                    <Link href={`/unlock-requests/rules/${rule.id}/edit`}>
                      <Button variant="ghost" size="icon">
                        <Pencil className="h-4 w-4" />
                      </Button>
                    </Link>

                    {/* Delete */}
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-red-600 hover:text-red-700"
                      onClick={() => handleDelete(rule.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
