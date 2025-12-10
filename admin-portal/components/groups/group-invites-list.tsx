"use client";

import { useState, useEffect, useCallback } from "react";
import type { GroupInvite, InviteStatus } from "@/types";
import { InviteStatusBadge } from "./invite-status-badge";
import { adminGroupsApi } from "@/lib/api-client";
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
  Mail,
  RefreshCw,
  Trash2,
  X,
  AlertTriangle,
  Copy,
  Check,
} from "lucide-react";

interface GroupInvitesListProps {
  groupId: string;
  groupName: string;
}

const STATUS_OPTIONS: { value: InviteStatus | "all"; label: string }[] = [
  { value: "all", label: "All Status" },
  { value: "pending", label: "Pending" },
  { value: "accepted", label: "Accepted" },
  { value: "expired", label: "Expired" },
  { value: "revoked", label: "Revoked" },
];

export function GroupInvitesList({ groupId, groupName }: GroupInvitesListProps) {
  const [statusFilter, setStatusFilter] = useState<InviteStatus | "all">("all");
  const [inviteToRevoke, setInviteToRevoke] = useState<GroupInvite | null>(null);
  const [showBulkRevoke, setShowBulkRevoke] = useState(false);
  const [copiedCode, setCopiedCode] = useState<string | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // Fetch invites
  const { data: invites, loading, error, execute } = useApi<GroupInvite[]>();

  // Revoke single invite - track success/failure via ref since void response returns null either way
  const revokeSuccessRef = { current: true };
  const { loading: revokeLoading, execute: executeRevoke } = useApi<void>({
    onError: () => { revokeSuccessRef.current = false; },
  });

  // Bulk revoke
  const { loading: bulkRevokeLoading, execute: executeBulkRevoke } = useApi<{ revoked_count: number }>();

  const fetchInvites = useCallback(() => {
    const status = statusFilter === "all" ? undefined : statusFilter;
    execute(() => adminGroupsApi.getInvites(groupId, status));
  }, [execute, groupId, statusFilter]);

  useEffect(() => {
    fetchInvites();
  }, [fetchInvites]);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleRevokeInvite = async () => {
    if (!inviteToRevoke) return;

    revokeSuccessRef.current = true; // Reset before call

    await executeRevoke(() =>
      adminGroupsApi.revokeInvite(groupId, inviteToRevoke.id)
    );

    setInviteToRevoke(null);

    if (revokeSuccessRef.current) {
      showNotification("success", "Invite revoked successfully");
      fetchInvites();
    } else {
      showNotification("error", "Failed to revoke invite. Please try again.");
    }
  };

  const handleBulkRevoke = async () => {
    const result = await executeBulkRevoke(() =>
      adminGroupsApi.revokeAllInvites(groupId)
    );

    if (result) {
      showNotification("success", `${result.revoked_count} invite(s) revoked`);
    } else {
      showNotification("error", "Failed to revoke invites. Please try again.");
    }
    setShowBulkRevoke(false);
    fetchInvites();
  };

  const copyToClipboard = async (code: string) => {
    try {
      await navigator.clipboard.writeText(code);
      setCopiedCode(code);
      setTimeout(() => setCopiedCode(null), 2000);
    } catch {
      // Fallback for browsers without clipboard API
      showNotification("error", "Failed to copy to clipboard");
    }
  };

  const formatDateTime = (dateString: string | null) => {
    if (!dateString) return "-";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const invitesList = invites || [];
  const pendingCount = invitesList.filter((i) => i.status === "pending").length;

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Group Invites</CardTitle>
              <CardDescription>
                {invitesList.length} invite{invitesList.length !== 1 ? "s" : ""} in {groupName}
                {pendingCount > 0 && ` (${pendingCount} pending)`}
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={fetchInvites}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
                />
                Refresh
              </Button>
              {pendingCount > 0 && (
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => setShowBulkRevoke(true)}
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  Revoke All ({pendingCount})
                </Button>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Notification */}
          {notification && (
            <div
              className={`mb-4 p-3 rounded-md ${
                notification.type === "success"
                  ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                  : "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400"
              }`}
            >
              {notification.message}
            </div>
          )}

          {/* Status Filter */}
          <div className="flex gap-4 mb-4">
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm min-w-[150px]"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value as InviteStatus | "all")}
            >
              {STATUS_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            {statusFilter !== "all" && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setStatusFilter("all")}
              >
                <X className="h-4 w-4 mr-1" />
                Clear Filter
              </Button>
            )}
          </div>

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={fetchInvites}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !invites && (
            <div className="space-y-3">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && invitesList.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Mail className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">
                {statusFilter !== "all"
                  ? `No ${statusFilter} invites`
                  : "No invites for this group"}
              </p>
            </div>
          )}

          {/* Invites Table */}
          {!error && invitesList.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Code</th>
                    <th className="text-left py-3 px-4 font-medium">Created By</th>
                    <th className="text-left py-3 px-4 font-medium">Status</th>
                    <th className="text-left py-3 px-4 font-medium">Expires</th>
                    <th className="text-left py-3 px-4 font-medium">Used By</th>
                    <th className="text-right py-3 px-4 font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {invitesList.map((invite) => (
                    <tr
                      key={invite.id}
                      className="border-b hover:bg-muted/50"
                    >
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-2">
                          <code className="text-sm bg-muted px-2 py-1 rounded">
                            {invite.code}
                          </code>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-6 w-6 p-0"
                            onClick={() => copyToClipboard(invite.code)}
                            title="Copy code"
                          >
                            {copiedCode === invite.code ? (
                              <Check className="h-3 w-3 text-green-500" />
                            ) : (
                              <Copy className="h-3 w-3" />
                            )}
                          </Button>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm">
                        <div>
                          <p>{invite.created_by_email}</p>
                          <p className="text-xs text-muted-foreground">
                            {formatDateTime(invite.created_at)}
                          </p>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <InviteStatusBadge status={invite.status} />
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {formatDateTime(invite.expires_at)}
                      </td>
                      <td className="py-3 px-4 text-sm">
                        {invite.used_by_email ? (
                          <div>
                            <p>{invite.used_by_email}</p>
                            <p className="text-xs text-muted-foreground">
                              {formatDateTime(invite.used_at)}
                            </p>
                          </div>
                        ) : (
                          <span className="text-muted-foreground">-</span>
                        )}
                      </td>
                      <td className="py-3 px-4 text-right">
                        {invite.status === "pending" && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setInviteToRevoke(invite)}
                            disabled={revokeLoading}
                            className="text-destructive hover:text-destructive"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Revoke Single Invite Confirmation */}
      {inviteToRevoke && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          aria-labelledby="revoke-invite-dialog-title"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setInviteToRevoke(null)}
            aria-hidden="true"
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <div className="flex items-start gap-3 mb-4">
              <AlertTriangle className="h-5 w-5 text-destructive mt-0.5" aria-hidden="true" />
              <div>
                <h2 id="revoke-invite-dialog-title" className="text-lg font-semibold">
                  Revoke Invite
                </h2>
                <p className="text-sm text-muted-foreground">
                  Are you sure you want to revoke this invite? The code{" "}
                  <code className="bg-muted px-1 rounded">{inviteToRevoke.code}</code>{" "}
                  will no longer be usable.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setInviteToRevoke(null)}
                disabled={revokeLoading}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleRevokeInvite}
                disabled={revokeLoading}
              >
                {revokeLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                Revoke
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Bulk Revoke Confirmation */}
      {showBulkRevoke && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          aria-labelledby="bulk-revoke-dialog-title"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setShowBulkRevoke(false)}
            aria-hidden="true"
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <div className="flex items-start gap-3 mb-4">
              <AlertTriangle className="h-5 w-5 text-destructive mt-0.5" aria-hidden="true" />
              <div>
                <h2 id="bulk-revoke-dialog-title" className="text-lg font-semibold">
                  Revoke All Pending Invites
                </h2>
                <p className="text-sm text-muted-foreground">
                  Are you sure you want to revoke all {pendingCount} pending invite{pendingCount !== 1 ? "s" : ""}?
                  They will no longer be usable.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setShowBulkRevoke(false)}
                disabled={bulkRevokeLoading}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleBulkRevoke}
                disabled={bulkRevokeLoading}
              >
                {bulkRevokeLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                Revoke All ({pendingCount})
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
