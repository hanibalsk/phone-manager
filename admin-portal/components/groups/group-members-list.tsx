"use client";

import { useState, useEffect, useCallback } from "react";
import type { GroupMember, GroupMemberRole } from "@/types";
import { MemberRoleBadge } from "./member-role-badge";
import { AddMemberDialog } from "./add-member-dialog";
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
  Users,
  RefreshCw,
  Trash2,
  UserPlus,
  Smartphone,
  AlertTriangle,
} from "lucide-react";

interface GroupMembersListProps {
  groupId: string;
  groupName: string;
}

export function GroupMembersList({ groupId, groupName }: GroupMembersListProps) {
  const [showAddDialog, setShowAddDialog] = useState(false);
  const [memberToRemove, setMemberToRemove] = useState<GroupMember | null>(null);
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  // Fetch members
  const { data: members, loading, error, execute } = useApi<GroupMember[]>();

  // Role change
  const { loading: roleLoading, execute: executeRoleChange } = useApi<GroupMember>();

  // Remove member - track success/failure via ref since void response returns null either way
  const removeSuccessRef = { current: true };
  const { loading: removeLoading, execute: executeRemove } = useApi<void>({
    onError: () => { removeSuccessRef.current = false; },
  });

  const fetchMembers = useCallback(() => {
    execute(() => adminGroupsApi.getMembers(groupId));
  }, [execute, groupId]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const showNotification = (type: "success" | "error", message: string) => {
    setNotification({ type, message });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleRoleChange = async (member: GroupMember, newRole: GroupMemberRole) => {
    const result = await executeRoleChange(() =>
      adminGroupsApi.changeMemberRole(groupId, member.id, newRole)
    );

    if (!result) {
      showNotification("error", "Failed to change role. Please try again.");
    } else {
      showNotification("success", `${member.user_name}'s role changed to ${newRole}`);
      fetchMembers();
    }
  };

  const handleRemoveMember = async () => {
    if (!memberToRemove) return;

    const memberName = memberToRemove.user_name;
    removeSuccessRef.current = true; // Reset before call

    await executeRemove(() =>
      adminGroupsApi.removeMember(groupId, memberToRemove.id)
    );

    setMemberToRemove(null);

    if (removeSuccessRef.current) {
      showNotification("success", `${memberName} removed from group`);
      fetchMembers();
    } else {
      showNotification("error", "Failed to remove member. Please try again.");
    }
  };

  const formatDateTime = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const membersList = members || [];

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>Group Members</CardTitle>
              <CardDescription>
                {membersList.length} member{membersList.length !== 1 ? "s" : ""} in {groupName}
              </CardDescription>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={fetchMembers}
                disabled={loading}
              >
                <RefreshCw
                  className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
                />
                Refresh
              </Button>
              <Button
                size="sm"
                onClick={() => setShowAddDialog(true)}
              >
                <UserPlus className="h-4 w-4 mr-2" />
                Add Member
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
                  ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                  : "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400"
              }`}
            >
              {notification.message}
            </div>
          )}

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
                <div key={i} className="flex items-center gap-4 py-3">
                  <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-48 bg-muted animate-pulse rounded" />
                  <div className="h-4 w-20 bg-muted animate-pulse rounded" />
                </div>
              ))}
            </div>
          )}

          {/* Empty State */}
          {!loading && !error && membersList.length === 0 && (
            <div className="flex flex-col items-center justify-center py-8 text-center">
              <Users className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground mb-4">No members in this group</p>
              <Button onClick={() => setShowAddDialog(true)}>
                <UserPlus className="h-4 w-4 mr-2" />
                Add First Member
              </Button>
            </div>
          )}

          {/* Members Table */}
          {!error && membersList.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4 font-medium">Member</th>
                    <th className="text-left py-3 px-4 font-medium">Role</th>
                    <th className="text-left py-3 px-4 font-medium">Devices</th>
                    <th className="text-left py-3 px-4 font-medium">Joined</th>
                    <th className="text-right py-3 px-4 font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {membersList.map((member) => (
                    <tr
                      key={member.id}
                      className="border-b hover:bg-muted/50"
                    >
                      <td className="py-3 px-4">
                        <div>
                          <p className="font-medium">{member.user_name}</p>
                          <p className="text-sm text-muted-foreground">{member.user_email}</p>
                        </div>
                      </td>
                      <td className="py-3 px-4">
                        <select
                          className="rounded-md border border-input bg-background px-2 py-1 text-sm"
                          value={member.role}
                          onChange={(e) =>
                            handleRoleChange(member, e.target.value as GroupMemberRole)
                          }
                          disabled={roleLoading}
                        >
                          <option value="member">Member</option>
                          <option value="admin">Admin</option>
                        </select>
                      </td>
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1">
                          <Smartphone className="h-4 w-4 text-muted-foreground" />
                          <span>{member.device_count}</span>
                        </div>
                      </td>
                      <td className="py-3 px-4 text-sm text-muted-foreground">
                        {formatDateTime(member.joined_at)}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setMemberToRemove(member)}
                          disabled={removeLoading}
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Add Member Dialog */}
      {showAddDialog && (
        <AddMemberDialog
          groupId={groupId}
          groupName={groupName}
          onClose={() => setShowAddDialog(false)}
          onMemberAdded={fetchMembers}
        />
      )}

      {/* Remove Member Confirmation */}
      {memberToRemove && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          aria-labelledby="remove-member-dialog-title"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setMemberToRemove(null)}
            aria-hidden="true"
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <div className="flex items-start gap-3 mb-4">
              <AlertTriangle className="h-5 w-5 text-destructive mt-0.5" aria-hidden="true" />
              <div>
                <h2 id="remove-member-dialog-title" className="text-lg font-semibold">
                  Remove Member
                </h2>
                <p className="text-sm text-muted-foreground">
                  Are you sure you want to remove &ldquo;{memberToRemove.user_name}&rdquo; from this group?
                  Their devices will no longer be part of this group.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2">
              <Button
                variant="outline"
                onClick={() => setMemberToRemove(null)}
                disabled={removeLoading}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleRemoveMember}
                disabled={removeLoading}
              >
                {removeLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                Remove
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
