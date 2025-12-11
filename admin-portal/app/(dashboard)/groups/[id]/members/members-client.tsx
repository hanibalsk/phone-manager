"use client";

import { useEffect } from "react";
import type { AdminGroup } from "@/types";
import { adminGroupsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { GroupMembersList } from "@/components/groups/group-members-list";
import { GroupStatusBadge } from "@/components/groups";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import {
  ArrowLeft,
  RefreshCw,
  Users,
} from "lucide-react";

interface Props {
  groupId: string;
}

export function GroupMembersClient({ groupId }: Props) {
  const { data: group, loading, error, execute } = useApi<AdminGroup>();

  useEffect(() => {
    execute(() => adminGroupsApi.get(groupId));
  }, [execute, groupId]);

  // Loading state
  if (loading && !group) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <div className="h-8 w-8 bg-muted animate-pulse rounded" />
          <div className="h-8 w-48 bg-muted animate-pulse rounded" />
        </div>
        <div className="h-64 bg-muted animate-pulse rounded" />
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Link href="/groups">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Groups
            </Button>
          </Link>
        </div>
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <p className="text-destructive mb-4">{error}</p>
          <Button variant="outline" onClick={() => execute(() => adminGroupsApi.get(groupId))}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Retry
          </Button>
        </div>
      </div>
    );
  }

  // Not found
  if (!group) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <Link href="/groups">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Groups
            </Button>
          </Link>
        </div>
        <div className="flex flex-col items-center justify-center py-12 text-center">
          <Users className="h-12 w-12 text-muted-foreground mb-4" />
          <p className="text-muted-foreground">Group not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col gap-4">
        <div className="flex items-center gap-4">
          <Link href="/groups">
            <Button variant="ghost" size="sm">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Back to Groups
            </Button>
          </Link>
        </div>
        <div className="flex items-center justify-between">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-bold">{group.name}</h1>
              <GroupStatusBadge status={group.status} />
            </div>
            <p className="text-muted-foreground">
              Organization: {group.organization_name} | Owner: {group.owner_name}
            </p>
          </div>
        </div>
      </div>

      {/* Members List */}
      <GroupMembersList groupId={groupId} groupName={group.name} />
    </div>
  );
}
