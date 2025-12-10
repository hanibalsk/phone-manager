"use client";

import type { DeviceDetails } from "@/types";
import { AdminDeviceStatusBadge } from "./admin-device-status-badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Activity, Clock, Shield } from "lucide-react";

interface AdminDeviceStatusProps {
  device: DeviceDetails;
}

export function AdminDeviceStatus({ device }: AdminDeviceStatusProps) {
  const formatDateTime = (dateString: string | null) => {
    if (!dateString) return "Never";
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const getRelativeTime = (dateString: string | null) => {
    if (!dateString) return "Unknown";
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return "Just now";
    if (diffMins < 60) return `${diffMins} minutes ago`;
    if (diffHours < 24) return `${diffHours} hours ago`;
    if (diffDays === 1) return "Yesterday";
    return `${diffDays} days ago`;
  };

  const getOnlineStatus = () => {
    if (!device.last_seen) return { label: "Unknown", color: "gray" };
    const lastSeen = new Date(device.last_seen);
    const now = new Date();
    const diffMins = Math.floor((now.getTime() - lastSeen.getTime()) / 60000);

    if (diffMins < 5) return { label: "Online", color: "green" };
    if (diffMins < 60) return { label: "Recently Active", color: "yellow" };
    return { label: "Offline", color: "gray" };
  };

  const onlineStatus = getOnlineStatus();

  const getEnrollmentStatusBadge = () => {
    const config = {
      enrolled: {
        label: "Enrolled",
        className: "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400",
      },
      pending: {
        label: "Pending",
        className: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400",
      },
      expired: {
        label: "Expired",
        className: "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400",
      },
    };
    return config[device.enrollment_status];
  };

  const enrollmentBadge = getEnrollmentStatusBadge();

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Activity className="h-5 w-5" />
          Device Status
        </CardTitle>
        <CardDescription>Current status and activity information</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="text-sm font-medium text-muted-foreground">
              Device Status
            </label>
            <div className="mt-1">
              <AdminDeviceStatusBadge status={device.status} />
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-muted-foreground">
              Online Status
            </label>
            <div className="mt-1 flex items-center gap-2">
              <span
                className={`inline-block h-2 w-2 rounded-full ${
                  onlineStatus.color === "green"
                    ? "bg-green-500"
                    : onlineStatus.color === "yellow"
                    ? "bg-yellow-500"
                    : "bg-gray-500"
                }`}
              />
              <span className="text-sm">{onlineStatus.label}</span>
            </div>
          </div>
        </div>

        <div className="border-t pt-4">
          <h4 className="text-sm font-medium mb-3 flex items-center gap-2">
            <Clock className="h-4 w-4" />
            Activity Timeline
          </h4>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="text-sm font-medium text-muted-foreground">
                Last Seen
              </label>
              <p className="text-sm">{formatDateTime(device.last_seen)}</p>
              <p className="text-xs text-muted-foreground">
                {getRelativeTime(device.last_seen)}
              </p>
            </div>
            <div>
              <label className="text-sm font-medium text-muted-foreground">
                Enrollment Status
              </label>
              <div className="mt-1">
                <span
                  className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${enrollmentBadge.className}`}
                >
                  {enrollmentBadge.label}
                </span>
              </div>
            </div>
          </div>
        </div>

        {device.policy_id && (
          <div className="border-t pt-4">
            <h4 className="text-sm font-medium mb-3 flex items-center gap-2">
              <Shield className="h-4 w-4" />
              Policy Compliance
            </h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm">Assigned Policy</span>
                <span className="text-sm font-medium">{device.policy_name}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm">Compliance Status</span>
                <span
                  className={`inline-flex items-center px-2 py-1 text-xs font-medium rounded-full ${
                    device.policy_compliant
                      ? "bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400"
                      : "bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400"
                  }`}
                >
                  {device.policy_compliant ? "Compliant" : "Non-Compliant"}
                </span>
              </div>
              {!device.policy_compliant && device.compliance_issues.length > 0 && (
                <div className="mt-2">
                  <label className="text-sm font-medium text-muted-foreground">
                    Compliance Issues
                  </label>
                  <ul className="mt-1 space-y-1">
                    {device.compliance_issues.map((issue, index) => (
                      <li
                        key={index}
                        className="text-sm text-red-600 dark:text-red-400 flex items-start gap-2"
                      >
                        <span className="mt-1">•</span>
                        {issue}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
