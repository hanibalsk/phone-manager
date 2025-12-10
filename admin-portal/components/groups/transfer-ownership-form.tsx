"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import type { AdminGroup, GroupMember } from "@/types";
import { adminGroupsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { MemberRoleBadge } from "./member-role-badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Users,
  RefreshCw,
  UserCog,
  Check,
  AlertTriangle,
} from "lucide-react";

interface TransferOwnershipFormProps {
  groupId: string;
  group: AdminGroup;
}

export function TransferOwnershipForm({ groupId, group }: TransferOwnershipFormProps) {
  const router = useRouter();
  const [selectedMemberId, setSelectedMemberId] = useState<string | null>(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // Fetch members
  const { data: members, loading, error, execute } = useApi<GroupMember[]>();

  // Transfer ownership
  const { loading: transferLoading, execute: executeTransfer } = useApi<AdminGroup>();

  const fetchMembers = useCallback(() => {
    execute(() => adminGroupsApi.getMembers(groupId));
  }, [execute, groupId]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    if (type === "error") {
      setTimeout(() => setNotification(null), 5000);
    }
  };

  const handleTransfer = async () => {
    if (!selectedMemberId) return;

    const result = await executeTransfer(() =>
      adminGroupsApi.transferOwnership(groupId, selectedMemberId)
    );

    if (!result) {
      showNotification("error", "Failed to transfer ownership. Please try again.");
      setShowConfirm(false);
    } else {
      showNotification("success", "Ownership transferred successfully!");
      // Redirect to group page after short delay
      setTimeout(() => {
        router.push("/groups");
      }, 1500);
    }
  };

  // Filter out the current owner from eligible members
  const eligibleMembers = (members || []).filter(
    (member) => member.user_id !== group.owner_id
  );

  const selectedMember = eligibleMembers.find((m) => m.id === selectedMemberId);

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Transfer Ownership</CardTitle>
          <CardDescription>
            Select a member to become the new owner of this group.
            The current owner will become an admin.
          </CardDescription>
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

          {/* Current Owner Info */}
          <div className="mb-6 p-4 border rounded-lg bg-muted/50">
            <h3 className="font-medium mb-2">Current Owner</h3>
            <div className="flex items-center gap-3">
              <div className="h-10 w-10 rounded-full bg-primary/10 flex items-center justify-center">
                <UserCog className="h-5 w-5 text-primary" />
              </div>
              <div>
                <p className="font-medium">{group.owner_name}</p>
                <p className="text-sm text-muted-foreground">{group.owner_email}</p>
              </div>
            </div>
          </div>

          {/* Error State */}
          {error && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <p className="text-destructive mb-4">{error}</p>
              <Button variant="outline" onClick={fetchMembers}>
                <RefreshCw className="h-4 w-4 mr-2" />
                Retry
              </Button>
            </div>
          )}

          {/* Loading State */}
          {loading && !members && (
            <div className="space-y-3">
              {[...Array(3)].map((_, i) => (
                <div key={i} className="flex items-center gap-4 py-3 px-4 border rounded-lg">
                  <div className="h-10 w-10 bg-muted animate-pulse rounded-full" />
                  <div className="flex-1">
                    <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                    <div className="h-3 w-48 bg-muted animate-pulse rounded mt-2" />
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* No Eligible Members */}
          {!loading && !error && eligibleMembers.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Users className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">
                No other members in this group to transfer ownership to.
                Add members first before transferring ownership.
              </p>
            </div>
          )}

          {/* Eligible Members List */}
          {!error && eligibleMembers.length > 0 && (
            <>
              <h3 className="font-medium mb-3">Select New Owner</h3>
              <div className="space-y-2 mb-6">
                {eligibleMembers.map((member) => (
                  <button
                    key={member.id}
                    className={`w-full flex items-center gap-3 p-4 border rounded-lg text-left transition-colors ${
                      selectedMemberId === member.id
                        ? "border-primary bg-primary/5"
                        : "hover:bg-muted/50"
                    }`}
                    onClick={() => setSelectedMemberId(member.id)}
                    disabled={transferLoading}
                  >
                    <div
                      className={`h-10 w-10 rounded-full flex items-center justify-center ${
                        selectedMemberId === member.id
                          ? "bg-primary text-primary-foreground"
                          : "bg-muted"
                      }`}
                    >
                      {selectedMemberId === member.id ? (
                        <Check className="h-5 w-5" />
                      ) : (
                        <Users className="h-5 w-5" />
                      )}
                    </div>
                    <div className="flex-1">
                      <p className="font-medium">{member.user_name}</p>
                      <p className="text-sm text-muted-foreground">{member.user_email}</p>
                    </div>
                    <MemberRoleBadge role={member.role} />
                  </button>
                ))}
              </div>

              <div className="flex justify-end gap-2">
                <Button
                  variant="outline"
                  onClick={() => router.push("/groups")}
                  disabled={transferLoading}
                >
                  Cancel
                </Button>
                <Button
                  onClick={() => setShowConfirm(true)}
                  disabled={!selectedMemberId || transferLoading}
                >
                  Transfer Ownership
                </Button>
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Confirmation Dialog */}
      {showConfirm && selectedMember && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-transfer-title"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setShowConfirm(false)}
            aria-hidden="true"
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <div className="flex items-start gap-3 mb-4">
              <AlertTriangle className="h-5 w-5 text-yellow-500 mt-0.5" aria-hidden="true" />
              <div>
                <h2 id="confirm-transfer-title" className="text-lg font-semibold">
                  Confirm Ownership Transfer
                </h2>
                <p className="text-sm text-muted-foreground mt-2">
                  Are you sure you want to transfer ownership of &ldquo;{group.name}&rdquo; to{" "}
                  <span className="font-medium">{selectedMember.user_name}</span>?
                </p>
                <p className="text-sm text-muted-foreground mt-2">
                  The current owner ({group.owner_name}) will become an Admin.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setShowConfirm(false)}
                disabled={transferLoading}
              >
                Cancel
              </Button>
              <Button
                onClick={handleTransfer}
                disabled={transferLoading}
              >
                {transferLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                Confirm Transfer
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
