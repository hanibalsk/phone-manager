"use client";

import { useState, useEffect, useCallback } from "react";
import type { AutoApprovalLogEntry, Organization, AutoApprovalRule } from "@/types";
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
import { Input } from "@/components/ui/input";
import {
  FileText,
  RefreshCw,
  Zap,
  Clock,
  User,
  Smartphone,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

export function AutoApprovalLog() {
  const [organizationId, setOrganizationId] = useState("");
  const [ruleId, setRuleId] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(1);

  const { data: logData, loading, error, execute: fetchLog } = useApi<{ items: AutoApprovalLogEntry[]; total: number }>();
  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: rulesData, execute: fetchRules } = useApi<{ items: AutoApprovalRule[] }>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
    fetchRules(() => unlockRequestsApi.listAutoApprovalRules());
  }, [fetchOrgs, fetchRules]);

  const loadLog = useCallback(() => {
    const params: Record<string, string | number | undefined> = {
      page,
      limit: ITEMS_PER_PAGE,
    };

    if (organizationId) params.organization_id = organizationId;
    if (ruleId) params.rule_id = ruleId;
    if (startDate) params.from = new Date(startDate).toISOString();
    if (endDate) params.to = new Date(endDate).toISOString();

    fetchLog(() => unlockRequestsApi.getAutoApprovalLog(params as { from?: string; to?: string; organization_id?: string; rule_id?: string; page?: number; limit?: number }));
  }, [fetchLog, organizationId, ruleId, startDate, endDate, page]);

  useEffect(() => {
    loadLog();
  }, [loadLog]);

  // Filter rules by organization
  const filteredRules = organizationId
    ? rulesData?.items?.filter(r => r.organization_id === organizationId)
    : rulesData?.items;

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString();
  };

  const formatDuration = (minutes: number) => {
    if (minutes >= 60) {
      const hours = Math.floor(minutes / 60);
      const mins = minutes % 60;
      return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
    }
    return `${minutes}m`;
  };

  const getOrgName = (orgId: string) => {
    return orgsData?.items?.find(o => o.id === orgId)?.name || orgId;
  };

  const log = logData?.items || [];
  const total = logData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FileText className="h-6 w-6" />
            <div>
              <CardTitle>Auto-Approval Audit Log</CardTitle>
              <CardDescription>
                Track all automatic approvals with rule details
              </CardDescription>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={loadLog}>
            <RefreshCw className="mr-2 h-4 w-4" />
            Refresh
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        {/* Filters */}
        <div className="flex flex-wrap gap-4 mb-6">
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={organizationId}
            onChange={(e) => {
              setOrganizationId(e.target.value);
              setRuleId("");
            }}
          >
            <option value="">All Organizations</option>
            {orgsData?.items?.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>

          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={ruleId}
            onChange={(e) => setRuleId(e.target.value)}
          >
            <option value="">All Rules</option>
            {filteredRules?.map((rule) => (
              <option key={rule.id} value={rule.id}>
                {rule.name}
              </option>
            ))}
          </select>

          <div className="flex items-center gap-2">
            <Input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-40"
            />
            <span className="text-muted-foreground">to</span>
            <Input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="w-40"
            />
          </div>
        </div>

        {/* Statistics Summary */}
        {log.length > 0 && (
          <div className="grid grid-cols-3 gap-4 mb-6">
            <div className="bg-purple-50 rounded-lg p-3 text-center">
              <div className="text-2xl font-bold text-purple-600">{total}</div>
              <div className="text-xs text-muted-foreground">Total Auto-Approvals</div>
            </div>
            <div className="bg-blue-50 rounded-lg p-3 text-center">
              <div className="text-2xl font-bold text-blue-600">
                {new Set(log.map(l => l.rule_name)).size}
              </div>
              <div className="text-xs text-muted-foreground">Rules Triggered</div>
            </div>
            <div className="bg-green-50 rounded-lg p-3 text-center">
              <div className="text-2xl font-bold text-green-600">
                {formatDuration(Math.round(log.reduce((sum, l) => sum + l.approved_duration_minutes, 0) / log.length) || 0)}
              </div>
              <div className="text-xs text-muted-foreground">Avg. Duration</div>
            </div>
          </div>
        )}

        {/* Log List */}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="text-muted-foreground">Loading log...</div>
          </div>
        ) : error ? (
          <div className="flex justify-center py-8 text-red-500">
            Error loading log
          </div>
        ) : log.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <FileText className="h-12 w-12 mb-4 opacity-50" />
            <p>No auto-approval log entries found</p>
          </div>
        ) : (
          <>
            <div className="space-y-3">
              {log.map((entry) => (
                <div
                  key={entry.id}
                  className="border rounded-lg p-4"
                >
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-2">
                      <span className="inline-flex items-center gap-1 text-xs text-purple-600 bg-purple-100 px-2 py-0.5 rounded-full">
                        <Zap className="h-3 w-3" />
                        Auto-Approved
                      </span>
                      <span className="text-sm font-medium text-primary">
                        {entry.rule_name}
                      </span>
                    </div>
                    <span className="text-sm text-muted-foreground">
                      {formatDate(entry.approved_at)}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
                    <div className="flex items-center gap-1">
                      <Smartphone className="h-4 w-4 text-muted-foreground" />
                      <span>{entry.device_name}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <User className="h-4 w-4 text-muted-foreground" />
                      <span>{entry.user_name}</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Clock className="h-4 w-4 text-muted-foreground" />
                      <span>Requested: {formatDuration(entry.requested_duration_minutes)}</span>
                    </div>
                    <div className="flex items-center gap-1 text-green-600">
                      <Clock className="h-4 w-4" />
                      <span>Approved: {formatDuration(entry.approved_duration_minutes)}</span>
                    </div>
                  </div>

                  <div className="mt-2 text-xs text-muted-foreground">
                    Organization: {getOrgName(entry.organization_id)}
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <div className="text-sm text-muted-foreground">
                  Showing {(page - 1) * ITEMS_PER_PAGE + 1} to{" "}
                  {Math.min(page * ITEMS_PER_PAGE, total)} of {total} entries
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 1}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === totalPages}
                    onClick={() => setPage(page + 1)}
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
