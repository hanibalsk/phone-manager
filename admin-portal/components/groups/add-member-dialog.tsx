"use client";

import { useState } from "react";
import type { GroupMemberRole, GroupMember, AdminUser, PaginatedResponse } from "@/types";
import { adminGroupsApi, usersApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  UserPlus,
  Search,
  RefreshCw,
  X,
} from "lucide-react";
import { useDebounce } from "@/hooks/use-debounce";

interface AddMemberDialogProps {
  groupId: string;
  groupName: string;
  onClose: () => void;
  onMemberAdded: () => void;
}

export function AddMemberDialog({
  groupId,
  groupName,
  onClose,
  onMemberAdded,
}: AddMemberDialogProps) {
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [selectedRole, setSelectedRole] = useState<GroupMemberRole>("member");
  const [error, setError] = useState<string | null>(null);

  const debouncedSearch = useDebounce(searchQuery, 300);

  // Search users
  const { data: usersData, loading: searchLoading, execute: searchUsers } = useApi<PaginatedResponse<AdminUser>>();

  // Add member
  const { loading: addLoading, execute: executeAdd } = useApi<GroupMember>();

  // Search users when query changes
  const handleSearch = () => {
    if (debouncedSearch.length >= 2) {
      searchUsers(() => usersApi.list({ search: debouncedSearch, limit: 10 }));
    }
  };

  // Effect to trigger search on debounced query change
  useState(() => {
    if (debouncedSearch.length >= 2) {
      handleSearch();
    }
  });

  const handleAddMember = async () => {
    if (!selectedUser) return;

    setError(null);
    const result = await executeAdd(() =>
      adminGroupsApi.addMember(groupId, selectedUser.id, selectedRole)
    );

    if (!result) {
      // Error is set on the hook state, we need to show a generic error
      setError("Failed to add member. Please try again.");
    } else {
      onMemberAdded();
      onClose();
    }
  };

  const searchResults = usersData?.items || [];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
      aria-labelledby="add-member-dialog-title"
    >
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
        aria-hidden="true"
      />
      <div className="relative bg-background rounded-lg shadow-lg w-full max-w-lg mx-4">
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b">
          <div className="flex items-center gap-2">
            <UserPlus className="h-5 w-5" />
            <h2 id="add-member-dialog-title" className="text-lg font-semibold">
              Add Member to {groupName}
            </h2>
          </div>
          <Button variant="ghost" size="sm" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        {/* Content */}
        <div className="p-4 space-y-4">
          {/* User Search */}
          <div>
            <label className="text-sm font-medium">Search User</label>
            <div className="relative mt-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
              <Input
                placeholder="Search by name or email..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setSelectedUser(null);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    handleSearch();
                  }
                }}
                className="pl-9"
              />
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              Type at least 2 characters and press Enter to search
            </p>
          </div>

          {/* Search Results */}
          {searchLoading && (
            <div className="flex items-center justify-center py-4">
              <RefreshCw className="h-4 w-4 animate-spin mr-2" />
              Searching...
            </div>
          )}

          {!searchLoading && searchResults.length > 0 && !selectedUser && (
            <div className="border rounded-md max-h-48 overflow-y-auto">
              {searchResults.map((user) => (
                <button
                  key={user.id}
                  className="w-full text-left px-3 py-2 hover:bg-muted flex items-center justify-between"
                  onClick={() => setSelectedUser(user)}
                >
                  <div>
                    <p className="font-medium">{user.display_name}</p>
                    <p className="text-sm text-muted-foreground">{user.email}</p>
                  </div>
                  <span className="text-xs text-muted-foreground">
                    {user.organization_name || "No org"}
                  </span>
                </button>
              ))}
            </div>
          )}

          {!searchLoading && searchResults.length === 0 && debouncedSearch.length >= 2 && (
            <p className="text-sm text-muted-foreground text-center py-4">
              No users found matching &ldquo;{debouncedSearch}&rdquo;
            </p>
          )}

          {/* Selected User */}
          {selectedUser && (
            <div className="border rounded-md p-3 bg-muted/50">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium">Selected User</p>
                  <p className="text-sm">{selectedUser.display_name}</p>
                  <p className="text-xs text-muted-foreground">{selectedUser.email}</p>
                </div>
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => setSelectedUser(null)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}

          {/* Role Selection */}
          {selectedUser && (
            <div>
              <label className="text-sm font-medium">Role</label>
              <select
                className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value as GroupMemberRole)}
              >
                <option value="member">Member</option>
                <option value="admin">Admin</option>
              </select>
            </div>
          )}

          {/* Error */}
          {error && (
            <p className="text-sm text-destructive">{error}</p>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end gap-2 p-4 border-t">
          <Button variant="outline" onClick={onClose} disabled={addLoading}>
            Cancel
          </Button>
          <Button
            onClick={handleAddMember}
            disabled={!selectedUser || addLoading}
          >
            {addLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
            Add Member
          </Button>
        </div>
      </div>
    </div>
  );
}
