"use client";

import { useState, useEffect, useCallback } from "react";
import type { RetentionPolicy, PurgeResult } from "@/types";
import { retentionApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import {
  RefreshCw,
  Edit,
  Trash2,
  HardDrive,
  Calendar,
  Clock,
} from "lucide-react";
import { RetentionPolicyForm } from "./retention-policy-form";

export function RetentionPolicyList() {
  const [editingPolicy, setEditingPolicy] = useState<RetentionPolicy | null>(null);
  const [policyToPurge, setPolicyToPurge] = useState<RetentionPolicy | null>(null);
  const [purgeResult, setPurgeResult] = useState<PurgeResult | null>(null);

  const { data: policies, loading, error, execute: fetchPolicies } = useApi<RetentionPolicy[]>();
  const { loading: updating, execute: updatePolicy } = useApi<RetentionPolicy>();
  const { loading: purging, execute: executePurge } = useApi<PurgeResult>();

  const loadPolicies = useCallback(() => {
    fetchPolicies(() => retentionApi.list());
  }, [fetchPolicies]);

  useEffect(() => {
    loadPolicies();
  }, [loadPolicies]);

  const handleToggleAutoDelete = async (policy: RetentionPolicy) => {
    await updatePolicy(() =>
      retentionApi.update(policy.organization_id, {
        auto_delete_enabled: !policy.auto_delete_enabled,
      })
    );
    loadPolicies();
  };

  const handleSavePolicy = async (policy: RetentionPolicy, data: { location_retention_days: number; event_retention_days: number; trip_retention_days: number }) => {
    await updatePolicy(() => retentionApi.update(policy.organization_id, data));
    setEditingPolicy(null);
    loadPolicies();
  };

  const handlePurge = async () => {
    if (!policyToPurge) return;

    const result = await executePurge(() => retentionApi.purge(policyToPurge.organization_id));
    if (result) {
      setPurgeResult(result);
    }
    loadPolicies();
  };

  const formatDays = (days: number) => {
    if (days < 30) return `${days} days`;
    if (days < 365) return `${Math.floor(days / 30)} months`;
    const years = Math.floor(days / 365);
    const remainingMonths = Math.floor((days % 365) / 30);
    return remainingMonths > 0 ? `${years}y ${remainingMonths}m` : `${years} year${years > 1 ? "s" : ""}`;
  };

  const formatStorage = (mb: number) => {
    if (mb < 1024) return `${mb.toFixed(1)} MB`;
    return `${(mb / 1024).toFixed(2)} GB`;
  };

  const formatDateTime = (dateString: string | null) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Data Retention Policies</CardTitle>
              <CardDescription>
                Configure how long data is retained for each organization
              </CardDescription>
            </div>
            <Button variant="outline" size="sm" onClick={loadPolicies} disabled={loading}>
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
              <Button variant="outline" onClick={loadPolicies}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !policies && (
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
          {!loading && !error && (!policies || policies.length === 0) && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <HardDrive className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No retention policies configured</p>
              <p className="text-sm text-muted-foreground mt-1">
                Policies will appear here once organizations are created
              </p>
            </div>
          )}

          {/* Policies Table */}
          {!error && policies && policies.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Organization</th>
                    <th className="text-left py-3 px-4 font-medium">Retention Periods</th>
                    <th className="text-left py-3 px-4 font-medium">Storage</th>
                    <th className="text-left py-3 px-4 font-medium">Auto-Delete</th>
                    <th className="text-left py-3 px-4 font-medium">Last Purge</th>
                    <th className="text-right py-3 px-4 font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {policies.map((policy) => (
                    <tr key={policy.organization_id} className="border-b hover:bg-muted/50">
                      <td className="py-3 px-4 font-medium">{policy.organization_name}</td>
                      <td className="py-3 px-4 text-sm">
                        <div className="space-y-1">
                          <div className="flex items-center gap-1">
                            <Calendar className="h-3 w-3 text-muted-foreground" />
                            <span>Locations: {formatDays(policy.location_retention_days)}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Calendar className="h-3 w-3 text-muted-foreground" />
                            <span>Events: {formatDays(policy.event_retention_days)}</span>
                          </div>
                          <div className="flex items-center gap-1">
                            <Calendar className="h-3 w-3 text-muted-foreground" />
                            <span>Trips: {formatDays(policy.trip_retention_days)}</span>
                          </div>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1.5">
                          <HardDrive className="h-4 w-4 text-muted-foreground" />
                          <span className="font-medium">{formatStorage(policy.storage_used_mb)}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <Switch
                          checked={policy.auto_delete_enabled}
                          onCheckedChange={() => handleToggleAutoDelete(policy)}
                          disabled={updating}
                        />
                      </td>
                      <td className="py-3 px-4 text-sm">
                        <div className="flex items-center gap-1">
                          <Clock className="h-4 w-4 text-muted-foreground" />
                          {formatDateTime(policy.last_purge_at)}
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            title="Edit retention periods"
                            onClick={() => setEditingPolicy(policy)}
                          >
                            <Edit className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            title="Purge expired data"
                            onClick={() => setPolicyToPurge(policy)}
                          >
                            <Trash2 className="h-4 w-4 text-orange-500" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Edit Policy Modal */}
      {editingPolicy && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setEditingPolicy(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-4">
              Edit Retention Policy - {editingPolicy.organization_name}
            </h2>
            <RetentionPolicyForm
              policy={editingPolicy}
              onSubmit={(data) => handleSavePolicy(editingPolicy, data)}
              onCancel={() => setEditingPolicy(null)}
              loading={updating}
            />
          </div>
        </div>
      )}

      {/* Purge Confirmation Modal */}
      {policyToPurge && !purgeResult && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setPolicyToPurge(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-2">Purge Expired Data</h2>
            <p className="text-sm text-muted-foreground mb-4">
              This will permanently delete all data older than the configured retention
              periods for <strong>{policyToPurge.organization_name}</strong>.
            </p>
            <div className="bg-orange-100 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800 rounded-md p-3 mb-4">
              <p className="text-sm text-orange-800 dark:text-orange-200 font-medium">
                Warning: This action cannot be undone!
              </p>
              <p className="text-xs text-orange-700 dark:text-orange-300 mt-1">
                Data older than the retention periods will be permanently deleted.
              </p>
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setPolicyToPurge(null)}
                disabled={purging}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handlePurge}
                disabled={purging}
              >
                {purging && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                Purge Data
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Purge Result Modal */}
      {purgeResult && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => {
              setPurgeResult(null);
              setPolicyToPurge(null);
            }}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-4">Purge Complete</h2>
            <div className="space-y-3 mb-6">
              <div className="flex justify-between items-center py-2 border-b">
                <span className="text-muted-foreground">Locations deleted</span>
                <span className="font-medium">{purgeResult.locations_deleted.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center py-2 border-b">
                <span className="text-muted-foreground">Events deleted</span>
                <span className="font-medium">{purgeResult.events_deleted.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center py-2 border-b">
                <span className="text-muted-foreground">Trips deleted</span>
                <span className="font-medium">{purgeResult.trips_deleted.toLocaleString()}</span>
              </div>
              <div className="flex justify-between items-center py-2 bg-green-100 dark:bg-green-900/20 px-3 rounded-md">
                <span className="text-green-800 dark:text-green-200 font-medium">Storage freed</span>
                <span className="font-bold text-green-800 dark:text-green-200">
                  {formatStorage(purgeResult.storage_freed_mb)}
                </span>
              </div>
            </div>
            <div className="flex justify-end">
              <Button
                onClick={() => {
                  setPurgeResult(null);
                  setPolicyToPurge(null);
                }}
              >
                Done
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
