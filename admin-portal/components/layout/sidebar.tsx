"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { usePermissions } from "@/hooks/use-permissions";
import {
  ORGANIZATIONS_READ,
  USERS_READ,
  ROLES_READ,
  DEVICES_READ,
  CONFIG_READ,
  CONFIG_UPDATE,
} from "@/lib/permissions";
import {
  Smartphone,
  Bell,
  Clock,
  Settings,
  LayoutDashboard,
  SlidersHorizontal,
  Users,
  Building2,
  Shield,
  type LucideIcon,
} from "lucide-react";

interface NavItem {
  name: string;
  href: string;
  icon: LucideIcon;
  /** Permission codes required to view this item (empty = always visible) */
  permissions: string[];
}

const navigation: NavItem[] = [
  { name: "Dashboard", href: "/", icon: LayoutDashboard, permissions: [] },
  { name: "Organizations", href: "/organizations", icon: Building2, permissions: [ORGANIZATIONS_READ] },
  { name: "Users", href: "/users", icon: Users, permissions: [USERS_READ] },
  { name: "Roles", href: "/roles", icon: Shield, permissions: [ROLES_READ] },
  { name: "Devices", href: "/devices", icon: Smartphone, permissions: [DEVICES_READ] },
  { name: "Unlock Requests", href: "/unlock-requests", icon: Bell, permissions: [DEVICES_READ] },
  { name: "App Limits", href: "/limits", icon: Clock, permissions: [DEVICES_READ] },
  { name: "Settings", href: "/settings", icon: Settings, permissions: [CONFIG_READ] },
  { name: "Configuration", href: "/config", icon: SlidersHorizontal, permissions: [CONFIG_UPDATE] },
];

export function Sidebar() {
  const pathname = usePathname();
  const { hasAnyPermission, isSuperAdmin } = usePermissions();

  // Filter navigation items based on user permissions
  const visibleNavigation = navigation.filter((item) => {
    // Items with no permission requirements are always visible
    if (item.permissions.length === 0) return true;
    // Super admins see everything
    if (isSuperAdmin) return true;
    // Check if user has any of the required permissions
    return hasAnyPermission(item.permissions);
  });

  return (
    <div className="flex h-full w-64 flex-col border-r bg-card">
      <div className="flex h-16 items-center border-b px-6">
        <h1 className="text-lg font-semibold">Phone Manager</h1>
      </div>
      <nav className="flex-1 space-y-1 p-4">
        {visibleNavigation.map((item) => {
          const isActive = pathname === item.href;
          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                isActive
                  ? "bg-primary text-primary-foreground"
                  : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.name}
            </Link>
          );
        })}
      </nav>
    </div>
  );
}
