"use client";

import type { GroupMemberRole } from "@/types";

interface MemberRoleBadgeProps {
  role: GroupMemberRole;
}

const roleConfig: Record<GroupMemberRole, { label: string; className: string }> = {
  admin: {
    label: "Admin",
    className: "bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400",
  },
  member: {
    label: "Member",
    className: "bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400",
  },
};

export function MemberRoleBadge({ role }: MemberRoleBadgeProps) {
  const config = roleConfig[role];

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${config.className}`}
    >
      {config.label}
    </span>
  );
}
