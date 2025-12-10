"use client";

import { useState } from "react";
import type { DeviceDetails } from "@/types";
import { adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Settings, Ban, RefreshCw, Trash2, AlertTriangle } from "lucide-react";

interface AdminDeviceActionsProps {
  device: DeviceDetails;
  onActionComplete: () => void;
  onDelete?: () => void;
}

type ActionType = "suspend" | "reactivate" | "delete" | null;

export function AdminDeviceActions({ device, onActionComplete, onDelete }: AdminDeviceActionsProps) {
  const [activeAction, setActiveAction] = useState<ActionType>(null);
  const [suspendReason, setSuspendReason] = useState("");
  const [confirmText, setConfirmText] = useState("");

  const { loading: suspendLoading, execute: executeSuspend } = useApi<DeviceDetails>();
  const { loading: reactivateLoading, execute: executeReactivate } = useApi<DeviceDetails>();
  const { loading: deleteLoading, execute: executeDelete } = useApi<{ success: boolean }>();

  const handleSuspend = async () => {
    const result = await executeSuspend(() => adminDevicesApi.suspend(device.id, suspendReason));
    if (result) {
      setActiveAction(null);
      setSuspendReason("");
      onActionComplete();
    }
  };

  const handleReactivate = async () => {
    const result = await executeReactivate(() => adminDevicesApi.reactivate(device.id));
    if (result) {
      setActiveAction(null);
      onActionComplete();
    }
  };

  const handleDelete = async () => {
    if (confirmText !== device.display_name) return;
    const result = await executeDelete(() => adminDevicesApi.delete(device.id));
    if (result) {
      setActiveAction(null);
      setConfirmText("");
      if (onDelete) {
        onDelete();
      } else {
        onActionComplete();
      }
    }
  };

  const isLoading = suspendLoading || reactivateLoading || deleteLoading;

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Settings className="h-5 w-5" />
          Device Actions
        </CardTitle>
        <CardDescription>Manage device status and lifecycle</CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Default view - action buttons */}
        {activeAction === null && (
          <div className="flex flex-wrap gap-3">
            {device.status !== "suspended" ? (
              <Button
                variant="outline"
                onClick={() => setActiveAction("suspend")}
                disabled={isLoading}
              >
                <Ban className="h-4 w-4 mr-2" />
                Suspend Device
              </Button>
            ) : (
              <Button
                variant="outline"
                onClick={() => setActiveAction("reactivate")}
                disabled={isLoading}
              >
                <RefreshCw className="h-4 w-4 mr-2" />
                Reactivate Device
              </Button>
            )}
            <Button
              variant="destructive"
              onClick={() => setActiveAction("delete")}
              disabled={isLoading}
            >
              <Trash2 className="h-4 w-4 mr-2" />
              Delete Device
            </Button>
          </div>
        )}

        {/* Suspend confirmation */}
        {activeAction === "suspend" && (
          <div className="p-4 border rounded-lg space-y-4">
            <div className="flex items-start gap-3">
              <Ban className="h-5 w-5 text-yellow-500 mt-0.5" />
              <div>
                <h4 className="font-medium">Suspend Device</h4>
                <p className="text-sm text-muted-foreground">
                  Suspending this device will prevent it from syncing data. The device can be
                  reactivated later.
                </p>
              </div>
            </div>
            <div>
              <label className="text-sm font-medium">Reason (optional)</label>
              <Input
                placeholder="Enter reason for suspension..."
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                className="mt-1"
              />
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setActiveAction(null);
                  setSuspendReason("");
                }}
                disabled={suspendLoading}
              >
                Cancel
              </Button>
              <Button
                variant="default"
                onClick={handleSuspend}
                disabled={suspendLoading}
              >
                {suspendLoading ? (
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                  <Ban className="h-4 w-4 mr-2" />
                )}
                Confirm Suspend
              </Button>
            </div>
          </div>
        )}

        {/* Reactivate confirmation */}
        {activeAction === "reactivate" && (
          <div className="p-4 border rounded-lg space-y-4">
            <div className="flex items-start gap-3">
              <RefreshCw className="h-5 w-5 text-green-500 mt-0.5" />
              <div>
                <h4 className="font-medium">Reactivate Device</h4>
                <p className="text-sm text-muted-foreground">
                  Reactivating this device will allow it to resume syncing data.
                </p>
              </div>
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => setActiveAction(null)}
                disabled={reactivateLoading}
              >
                Cancel
              </Button>
              <Button
                variant="default"
                onClick={handleReactivate}
                disabled={reactivateLoading}
              >
                {reactivateLoading ? (
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                  <RefreshCw className="h-4 w-4 mr-2" />
                )}
                Confirm Reactivate
              </Button>
            </div>
          </div>
        )}

        {/* Delete confirmation */}
        {activeAction === "delete" && (
          <div className="p-4 border border-destructive/50 rounded-lg space-y-4 bg-destructive/5">
            <div className="flex items-start gap-3">
              <AlertTriangle className="h-5 w-5 text-destructive mt-0.5" />
              <div>
                <h4 className="font-medium text-destructive">Delete Device</h4>
                <p className="text-sm text-muted-foreground">
                  This action is permanent and cannot be undone. All device data, location
                  history, and associated records will be permanently deleted.
                </p>
              </div>
            </div>
            <div>
              <label className="text-sm font-medium">
                Type <span className="font-mono">{device.display_name}</span> to confirm
              </label>
              <Input
                placeholder="Enter device name..."
                value={confirmText}
                onChange={(e) => setConfirmText(e.target.value)}
                className="mt-1"
              />
            </div>
            <div className="flex gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  setActiveAction(null);
                  setConfirmText("");
                }}
                disabled={deleteLoading}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleDelete}
                disabled={deleteLoading || confirmText !== device.display_name}
              >
                {deleteLoading ? (
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                  <Trash2 className="h-4 w-4 mr-2" />
                )}
                Delete Permanently
              </Button>
            </div>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
