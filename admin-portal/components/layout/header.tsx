"use client";

import { Bell, LogOut, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/contexts/auth-context";

interface HeaderProps {
  title?: string;
}

export function Header({ title = "Dashboard" }: HeaderProps) {
  const { user, logout } = useAuth();

  const handleLogout = async () => {
    await logout();
  };

  return (
    <header className="flex h-16 items-center justify-between border-b bg-card px-6">
      <h2 className="text-xl font-semibold">{title}</h2>
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" aria-label="Notifications">
          <Bell className="h-5 w-5" />
        </Button>
        <div className="flex items-center gap-2">
          <div className="flex items-center gap-2 px-2">
            <User className="h-5 w-5 text-muted-foreground" />
            <span className="text-sm font-medium">
              {user?.display_name || user?.email || "User"}
            </span>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={handleLogout}
            aria-label="Sign out"
            title="Sign out"
          >
            <LogOut className="h-5 w-5" />
          </Button>
        </div>
      </div>
    </header>
  );
}
