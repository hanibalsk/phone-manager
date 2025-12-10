"use client";

import type { AppCategory } from "@/types";
import {
  MessageCircle,
  Gamepad2,
  Briefcase,
  Film,
  GraduationCap,
  Phone,
  MoreHorizontal,
} from "lucide-react";

const categoryConfig: Record<
  AppCategory,
  { label: string; className: string; Icon: React.ComponentType<{ className?: string }> }
> = {
  social: {
    label: "Social",
    className: "bg-pink-100 text-pink-800",
    Icon: MessageCircle,
  },
  games: {
    label: "Games",
    className: "bg-purple-100 text-purple-800",
    Icon: Gamepad2,
  },
  productivity: {
    label: "Productivity",
    className: "bg-blue-100 text-blue-800",
    Icon: Briefcase,
  },
  entertainment: {
    label: "Entertainment",
    className: "bg-orange-100 text-orange-800",
    Icon: Film,
  },
  education: {
    label: "Education",
    className: "bg-green-100 text-green-800",
    Icon: GraduationCap,
  },
  communication: {
    label: "Communication",
    className: "bg-cyan-100 text-cyan-800",
    Icon: Phone,
  },
  other: {
    label: "Other",
    className: "bg-gray-100 text-gray-800",
    Icon: MoreHorizontal,
  },
};

interface AppCategoryBadgeProps {
  category: AppCategory;
  showIcon?: boolean;
  compact?: boolean;
}

export function AppCategoryBadge({ category, showIcon = true, compact = false }: AppCategoryBadgeProps) {
  const config = categoryConfig[category] || categoryConfig.other;
  const { Icon } = config;

  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full px-2 py-1 text-xs font-medium ${config.className}`}
    >
      {showIcon && <Icon className="h-3 w-3" />}
      {!compact && config.label}
    </span>
  );
}
