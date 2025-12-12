"use client";

import { useState, useEffect, useCallback } from "react";
import type { EnrollmentToken, PaginatedResponse } from "@/types";
import { TokenStatusBadge } from "./token-status-badge";
import { CreateTokenDialog } from "./create-token-dialog";
import { TokenQrDialog } from "./token-qr-dialog";
import { TokenUsageDialog } from "./token-usage-dialog";
import { enrollmentApi } from "@/lib/api-client";
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
  Key,
  RefreshCw,
  Plus,
  QrCode,
  Users,
  Trash2,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

export function TokenList() {
  // Dialog states
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [selectedTokenForQr, setSelectedTokenForQr] = useState<EnrollmentToken | null>(null);
  const [selectedTokenForUsage, setSelectedTokenForUsage] = useState<EnrollmentToken | null>(null);
  const [tokenToRevoke, setTokenToRevoke] = useState<EnrollmentToken | null>(null);

  // Pagination state
  const [currentPage, setCurrentPage] = useState(1);

  // API state
  const { data, loading, error, execute } = useApi<PaginatedResponse<EnrollmentToken>>();
  const { loading: revokeLoading, execute: executeRevoke } = useApi<{ success: boolean }>();

  // Fetch tokens
  const fetchTokens = useCallback(() => {
    execute(() => enrollmentApi.list({ page: currentPage, limit: ITEMS_PER_PAGE }));
  }, [execute, currentPage]);

  useEffect(() => {
    fetchTokens();
  }, [fetchTokens]);

  const tokens = data?.items || [];
  const totalTokens = data?.total || 0;
  const totalPages = Math.ceil(totalTokens / ITEMS_PER_PAGE);

  const handleCreateSuccess = (token: EnrollmentToken) => {
    setShowCreateDialog(false);
    setSelectedTokenForQr(token); // Show QR code after creation
    fetchTokens();
  };

  const handleRevoke = async () => {
    if (!tokenToRevoke) return;
    const result = await executeRevoke(() => enrollmentApi.revoke(tokenToRevoke.id));
    if (result) {
      setTokenToRevoke(null);
      fetchTokens();
    }
  };

  const formatDate = (dateString: string | null) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <>
      <Card data-testid="token-list">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Enrollment Tokens</CardTitle>
              <CardDescription>
                Manage device enrollment tokens
                {totalTokens > 0 && ` • ${totalTokens} total tokens`}
              </CardDescription>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={fetchTokens}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
                />
                Refresh
              </Button>
              <Button size="sm" onClick={() => setShowCreateDialog(true)} data-testid="create-token-btn">
                <Plus className="h-4 w-4 mr-2" />
                Create Token
              </Button>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={fetchTokens}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !data && (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-24 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-24 bg-muted animate-pulse rounded" />
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && tokens.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Key className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground mb-4">
                No enrollment tokens found
              </p>
              <Button onClick={() => setShowCreateDialog(true)}>
                <Plus className="h-4 w-4 mr-2" />
                Create Your First Token
              </Button>
            </div>
          )}

          {/* Token Table */}
          {!error && tokens.length > 0 && (
            <>
              <div className="overflow-x-auto">
                <table className="w-full" data-testid="token-table">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left py-3 px-4 font-medium">Name</th>
                      <th className="text-left py-3 px-4 font-medium">Code</th>
                      <th className="text-left py-3 px-4 font-medium">Uses</th>
                      <th className="text-left py-3 px-4 font-medium">Expires</th>
                      <th className="text-left py-3 px-4 font-medium">Status</th>
                      <th className="text-left py-3 px-4 font-medium">Created</th>
                      <th className="text-right py-3 px-4 font-medium">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tokens.map((token) => (
                      <tr
                        key={token.id}
                        className="border-b hover:bg-muted/50"
                        data-testid={`token-row-${token.id}`}
                      >
                        <td className="py-3 px-4">
                          <div className="flex items-center gap-2">
                            <Key className="h-4 w-4 text-muted-foreground" />
                            <span className="font-medium">{token.name}</span>
                          </div>
                        </td>
                        <td className="py-3 px-4">
                          <code className="text-xs bg-muted px-2 py-1 rounded">
                            {token.code.slice(0, 8)}...
                          </code>
                        </td>
                        <td className="py-3 px-4 text-sm">
                          {token.uses_count} / {token.max_uses ?? "∞"}
                        </td>
                        <td className="py-3 px-4 text-sm text-muted-foreground">
                          {formatDate(token.expires_at)}
                        </td>
                        <td className="py-3 px-4">
                          <TokenStatusBadge status={token.status} />
                        </td>
                        <td className="py-3 px-4 text-sm text-muted-foreground">
                          {formatDateTime(token.created_at)}
                        </td>
                        <td className="py-3 px-4 text-right">
                          <div className="flex items-center justify-end gap-1">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setSelectedTokenForQr(token)}
                              title="Show QR Code"
                            >
                              <QrCode className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setSelectedTokenForUsage(token)}
                              title="View Usage"
                            >
                              <Users className="h-4 w-4" />
                            </Button>
                            {token.status === "active" && (
                              <Button
                                variant="ghost"
                                size="sm"
                                onClick={() => setTokenToRevoke(token)}
                                title="Revoke Token"
                                className="text-destructive hover:text-destructive"
                              >
                                <Trash2 className="h-4 w-4" />
                              </Button>
                            )}
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
                    Showing {(currentPage - 1) * ITEMS_PER_PAGE + 1} to{" "}
                    {Math.min(currentPage * ITEMS_PER_PAGE, totalTokens)} of{" "}
                    {totalTokens} tokens
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                      disabled={currentPage === 1 || loading}
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Previous
                    </Button>
                    <span className="text-sm text-muted-foreground">
                      Page {currentPage} of {totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                      disabled={currentPage === totalPages || loading}
                    >
                      Next
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Dialogs */}
      {showCreateDialog && (
        <CreateTokenDialog
          onSuccess={handleCreateSuccess}
          onCancel={() => setShowCreateDialog(false)}
        />
      )}

      {selectedTokenForQr && (
        <TokenQrDialog
          token={selectedTokenForQr}
          onClose={() => setSelectedTokenForQr(null)}
        />
      )}

      {selectedTokenForUsage && (
        <TokenUsageDialog
          token={selectedTokenForUsage}
          onClose={() => setSelectedTokenForUsage(null)}
        />
      )}

      {/* Revoke Confirmation */}
      {tokenToRevoke && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setTokenToRevoke(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-2">Revoke Token</h2>
            <p className="text-sm text-muted-foreground mb-4">
              Are you sure you want to revoke the token &quot;{tokenToRevoke.name}&quot;?
              This action cannot be undone. Existing enrolled devices will not be affected.
            </p>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setTokenToRevoke(null)}
                disabled={revokeLoading}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleRevoke}
                disabled={revokeLoading}
              >
                {revokeLoading ? (
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                  <Trash2 className="h-4 w-4 mr-2" />
                )}
                Revoke
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
