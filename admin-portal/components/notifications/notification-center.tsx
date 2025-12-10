"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Bell,
  Check,
  CheckCheck,
  Trash2,
  RefreshCw,
  Info,
  CheckCircle,
  AlertTriangle,
  XCircle,
  Settings,
  User,
  Building,
  Smartphone,
  Shield,
  Clock,
  X,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { notificationsApi } from "@/lib/api-client";
import type { Notification, NotificationType, NotificationCategory } from "@/types";

const TYPE_CONFIG: Record<
  NotificationType,
  { icon: React.ComponentType<{ className?: string }>; color: string }
> = {
  info: { icon: Info, color: "text-blue-500" },
  success: { icon: CheckCircle, color: "text-green-500" },
  warning: { icon: AlertTriangle, color: "text-yellow-500" },
  error: { icon: XCircle, color: "text-red-500" },
  system: { icon: Settings, color: "text-purple-500" },
};

const CATEGORY_ICONS: Record<NotificationCategory, React.ComponentType<{ className?: string }>> = {
  user: User,
  organization: Building,
  device: Smartphone,
  security: Shield,
  system: Settings,
  audit: Clock,
};

interface NotificationCenterProps {
  onClose?: () => void;
}

export function NotificationCenter({ onClose }: NotificationCenterProps) {
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [showUnreadOnly, setShowUnreadOnly] = useState(false);

  const { execute: fetchNotifications, loading } = useApi<Notification[]>();
  const { execute: fetchUnreadCount } = useApi<{ count: number }>();
  const { execute: markRead } = useApi<void>();
  const { execute: markAllRead } = useApi<void>();
  const { execute: deleteNotification } = useApi<void>();

  const loadNotifications = async () => {
    const result = await fetchNotifications(() => notificationsApi.getAll(showUnreadOnly));
    if (result) {
      setNotifications(result);
    }
  };

  const loadUnreadCount = async () => {
    const result = await fetchUnreadCount(() => notificationsApi.getUnreadCount());
    if (result) {
      setUnreadCount(result.count);
    }
  };

  useEffect(() => {
    loadNotifications();
    loadUnreadCount();
  }, [showUnreadOnly]);

  const handleMarkRead = async (id: string) => {
    await markRead(() => notificationsApi.markAsRead(id));
    loadNotifications();
    loadUnreadCount();
  };

  const handleMarkAllRead = async () => {
    await markAllRead(() => notificationsApi.markAllAsRead());
    loadNotifications();
    loadUnreadCount();
  };

  const handleDelete = async (id: string) => {
    await deleteNotification(() => notificationsApi.delete(id));
    loadNotifications();
    loadUnreadCount();
  };

  const displayedNotifications = notifications.slice(0, 10);

  return (
    <div className="w-[380px] max-h-[500px] flex flex-col bg-background border rounded-lg shadow-lg">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b">
        <div className="flex items-center gap-2">
          <h3 className="font-semibold">Notifications</h3>
          {unreadCount > 0 && (
            <Badge variant="secondary" className="text-xs">
              {unreadCount} unread
            </Badge>
          )}
        </div>
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={handleMarkAllRead}
            disabled={unreadCount === 0}
            title="Mark all as read"
          >
            <CheckCheck className="h-4 w-4" />
          </Button>
          <Link href="/settings/notifications">
            <Button variant="ghost" size="icon" className="h-8 w-8" title="Settings">
              <Settings className="h-4 w-4" />
            </Button>
          </Link>
          {onClose && (
            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>

      {/* Filter */}
      <div className="px-4 py-2 border-b">
        <div className="flex gap-2">
          <Button
            variant={!showUnreadOnly ? "secondary" : "ghost"}
            size="sm"
            onClick={() => setShowUnreadOnly(false)}
          >
            All
          </Button>
          <Button
            variant={showUnreadOnly ? "secondary" : "ghost"}
            size="sm"
            onClick={() => setShowUnreadOnly(true)}
          >
            Unread
          </Button>
        </div>
      </div>

      {/* Notifications List */}
      <div className="flex-1 overflow-y-auto">
        {loading ? (
          <div className="flex items-center justify-center py-8">
            <RefreshCw className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        ) : displayedNotifications.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-8 text-muted-foreground">
            <Bell className="h-10 w-10 mb-2" />
            <p className="text-sm">No notifications</p>
          </div>
        ) : (
          <div className="divide-y">
            {displayedNotifications.map((notification) => {
              const TypeIcon = TYPE_CONFIG[notification.type].icon;
              const CategoryIcon = CATEGORY_ICONS[notification.category];

              return (
                <div
                  key={notification.id}
                  className={`p-3 hover:bg-muted/50 transition-colors ${
                    !notification.read ? "bg-muted/30" : ""
                  }`}
                >
                  <div className="flex gap-3">
                    <div className={`mt-0.5 ${TYPE_CONFIG[notification.type].color}`}>
                      <TypeIcon className="h-5 w-5" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex-1 min-w-0">
                          <p className="font-medium text-sm truncate">
                            {notification.title}
                          </p>
                          <p className="text-xs text-muted-foreground line-clamp-2 mt-0.5">
                            {notification.message}
                          </p>
                        </div>
                        <div className="flex items-center gap-1 shrink-0">
                          {!notification.read && (
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-6 w-6"
                              onClick={() => handleMarkRead(notification.id)}
                              title="Mark as read"
                            >
                              <Check className="h-3 w-3" />
                            </Button>
                          )}
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-6 w-6 text-muted-foreground hover:text-destructive"
                            onClick={() => handleDelete(notification.id)}
                            title="Delete"
                          >
                            <Trash2 className="h-3 w-3" />
                          </Button>
                        </div>
                      </div>
                      <div className="flex items-center gap-2 mt-2">
                        <Badge variant="outline" className="text-xs px-1.5 py-0">
                          <CategoryIcon className="h-3 w-3 mr-1" />
                          {notification.category}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {formatTimeAgo(notification.created_at)}
                        </span>
                      </div>
                      {notification.link && (
                        <Link
                          href={notification.link}
                          className="text-xs text-primary hover:underline mt-1 inline-block"
                          onClick={onClose}
                        >
                          View details
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Footer */}
      {notifications.length > 10 && (
        <div className="p-3 border-t text-center">
          <Link href="/settings/notifications" onClick={onClose}>
            <Button variant="ghost" size="sm">
              View all notifications
            </Button>
          </Link>
        </div>
      )}
    </div>
  );
}

function formatTimeAgo(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24) return `${hours}h ago`;
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString();
}

// Notification Bell with Badge
interface NotificationBellProps {
  onClick?: () => void;
}

export function NotificationBell({ onClick }: NotificationBellProps) {
  const [unreadCount, setUnreadCount] = useState(0);
  const { execute: fetchUnreadCount } = useApi<{ count: number }>();

  useEffect(() => {
    const loadCount = async () => {
      const result = await fetchUnreadCount(() => notificationsApi.getUnreadCount());
      if (result) {
        setUnreadCount(result.count);
      }
    };
    loadCount();

    // Poll for updates every 30 seconds
    const interval = setInterval(loadCount, 30000);
    return () => clearInterval(interval);
  }, []);

  return (
    <Button
      variant="ghost"
      size="icon"
      className="relative"
      onClick={onClick}
      title="Notifications"
    >
      <Bell className="h-5 w-5" />
      {unreadCount > 0 && (
        <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-red-500 text-white text-xs flex items-center justify-center font-medium">
          {unreadCount > 9 ? "9+" : unreadCount}
        </span>
      )}
    </Button>
  );
}
