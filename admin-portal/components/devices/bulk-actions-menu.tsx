"use client";

import { useState } from "react";
import type { AdminDevice, BulkOperationResult } from "@/types";
import { adminDevicesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import {
  Ban,
  RefreshCw,
  Trash2,
  ChevronDown,
  AlertTriangle,
  CheckCircle,
  XCircle,
} from "lucide-react";

interface BulkActionsMenuProps {
  selectedDevices: AdminDevice[];
  onActionComplete: () => void;
  onClearSelection: () => void;
}

type BulkActionType = "suspend" | "reactivate" | "delete" | null;

export function BulkActionsMenu({
  selectedDevices,
  onActionComplete,
  onClearSelection,
}: BulkActionsMenuProps) {
  const [showMenu, setShowMenu] = useState(false);
  const [activeAction, setActiveAction] = useState<BulkActionType>(null);
  const [result, setResult] = useState<BulkOperationResult | null>(null);

  const { loading: suspendLoading, execute: executeSuspend } = useApi<BulkOperationResult>();
  const { loading: reactivateLoading, execute: executeReactivate } = useApi<BulkOperationResult>();
  const { loading: deleteLoading, execute: executeDelete } = useApi<BulkOperationResult>();

  const deviceIds = selectedDevices.map((d) => d.id);
  const isLoading = suspendLoading || reactivateLoading || deleteLoading;

  // Check device statuses for appropriate actions
  const activeCount = selectedDevices.filter((d) => d.status === "active").length;
  const suspendedCount = selectedDevices.filter((d) => d.status === "suspended").length;

  const handleSuspend = async () => {
    const res = await executeSuspend(() => adminDevicesApi.bulkSuspend(deviceIds));
    if (res) {
      setResult(res);
      onActionComplete();
    }
  };

  const handleReactivate = async () => {
    const res = await executeReactivate(() => adminDevicesApi.bulkReactivate(deviceIds));
    if (res) {
      setResult(res);
      onActionComplete();
    }
  };

  const handleDelete = async () => {
    const res = await executeDelete(() => adminDevicesApi.bulkDelete(deviceIds));
    if (res) {
      setResult(res);
      onActionComplete();
    }
  };

  const closeResult = () => {
    setResult(null);
    setActiveAction(null);
    onClearSelection();
  };

  if (selectedDevices.length === 0) {
    return null;
  }

  return (
    <>
      {/* Bulk Actions Button */}
      <div className="relative">
        <Button
          variant="outline"
          size="sm"
          onClick={() => setShowMenu(!showMenu)}
          disabled={isLoading}
        >
          Bulk Actions ({selectedDevices.length})
          <ChevronDown className="h-4 w-4 ml-2" />
        </Button>

        {/* Dropdown Menu */}
        {showMenu && (
          <>
            <div
              className="fixed inset-0 z-10"
              onClick={() => setShowMenu(false)}
            />
            <div className="absolute right-0 mt-1 w-48 rounded-md border bg-background shadow-lg z-20">
              <div className="py-1">
                <button
                  className="flex items-center w-full px-4 py-2 text-sm hover:bg-muted disabled:opacity-50"
                  onClick={() => {
                    setShowMenu(false);
                    setActiveAction("suspend");
                  }}
                  disabled={activeCount === 0}
                >
                  <Ban className="h-4 w-4 mr-2" />
                  Suspend ({activeCount})
                </button>
                <button
                  className="flex items-center w-full px-4 py-2 text-sm hover:bg-muted disabled:opacity-50"
                  onClick={() => {
                    setShowMenu(false);
                    setActiveAction("reactivate");
                  }}
                  disabled={suspendedCount === 0}
                >
                  <RefreshCw className="h-4 w-4 mr-2" />
                  Reactivate ({suspendedCount})
                </button>
                <button
                  className="flex items-center w-full px-4 py-2 text-sm text-destructive hover:bg-muted"
                  onClick={() => {
                    setShowMenu(false);
                    setActiveAction("delete");
                  }}
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete ({selectedDevices.length})
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      {/* Confirmation Dialogs */}
      {activeAction && !result && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setActiveAction(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            {activeAction === "suspend" && (
              <>
                <div className="flex items-start gap-3 mb-4">
                  <Ban className="h-5 w-5 text-yellow-500 mt-0.5" />
                  <div>
                    <h2 className="text-lg font-semibold">Suspend Devices</h2>
                    <p className="text-sm text-muted-foreground">
                      Are you sure you want to suspend {activeCount} device{activeCount !== 1 ? "s" : ""}?
                      Suspended devices will not be able to sync data.
                    </p>
                  </div>
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={() => setActiveAction(null)} disabled={suspendLoading}>
                    Cancel
                  </Button>
                  <Button onClick={handleSuspend} disabled={suspendLoading}>
                    {suspendLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                    Suspend
                  </Button>
                </div>
              </>
            )}

            {activeAction === "reactivate" && (
              <>
                <div className="flex items-start gap-3 mb-4">
                  <RefreshCw className="h-5 w-5 text-green-500 mt-0.5" />
                  <div>
                    <h2 className="text-lg font-semibold">Reactivate Devices</h2>
                    <p className="text-sm text-muted-foreground">
                      Are you sure you want to reactivate {suspendedCount} device{suspendedCount !== 1 ? "s" : ""}?
                      Reactivated devices will be able to resume syncing data.
                    </p>
                  </div>
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={() => setActiveAction(null)} disabled={reactivateLoading}>
                    Cancel
                  </Button>
                  <Button onClick={handleReactivate} disabled={reactivateLoading}>
                    {reactivateLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                    Reactivate
                  </Button>
                </div>
              </>
            )}

            {activeAction === "delete" && (
              <>
                <div className="flex items-start gap-3 mb-4">
                  <AlertTriangle className="h-5 w-5 text-destructive mt-0.5" />
                  <div>
                    <h2 className="text-lg font-semibold text-destructive">Delete Devices</h2>
                    <p className="text-sm text-muted-foreground">
                      This action is permanent and cannot be undone. All data for {selectedDevices.length} device{selectedDevices.length !== 1 ? "s" : ""} will be permanently deleted.
                    </p>
                  </div>
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={() => setActiveAction(null)} disabled={deleteLoading}>
                    Cancel
                  </Button>
                  <Button variant="destructive" onClick={handleDelete} disabled={deleteLoading}>
                    {deleteLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                    Delete Permanently
                  </Button>
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* Result Dialog */}
      {result && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div
            className="absolute inset-0 bg-black/50"
            onClick={closeResult}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <h2 className="text-lg font-semibold mb-4">Operation Complete</h2>

            <div className="space-y-3 mb-4">
              <div className="flex items-center justify-between p-3 rounded-lg bg-green-50 dark:bg-green-900/20">
                <div className="flex items-center gap-2">
                  <CheckCircle className="h-5 w-5 text-green-600 dark:text-green-400" />
                  <span className="font-medium">Successful</span>
                </div>
                <span className="font-bold text-green-600 dark:text-green-400">
                  {result.success_count}
                </span>
              </div>

              {result.failure_count > 0 && (
                <div className="flex items-center justify-between p-3 rounded-lg bg-red-50 dark:bg-red-900/20">
                  <div className="flex items-center gap-2">
                    <XCircle className="h-5 w-5 text-red-600 dark:text-red-400" />
                    <span className="font-medium">Failed</span>
                  </div>
                  <span className="font-bold text-red-600 dark:text-red-400">
                    {result.failure_count}
                  </span>
                </div>
              )}
            </div>

            {result.failures.length > 0 && (
              <div className="mb-4">
                <p className="text-sm font-medium mb-2">Failed Devices:</p>
                <div className="max-h-32 overflow-auto space-y-1">
                  {result.failures.map((failure) => (
                    <div
                      key={failure.device_id}
                      className="text-sm p-2 rounded bg-muted"
                    >
                      <span className="font-medium">{failure.device_name}</span>
                      <span className="text-muted-foreground"> - {failure.error}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <Button className="w-full" onClick={closeResult}>
              Close
            </Button>
          </div>
        </div>
      )}
    </>
  );
}
