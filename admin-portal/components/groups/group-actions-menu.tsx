"use client";

import { useState } from "react";
import type { AdminGroup } from "@/types";
import { adminGroupsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import {
  MoreHorizontal,
  Users,
  UserCog,
  Ban,
  RefreshCw,
  Archive,
  AlertTriangle,
} from "lucide-react";
import Link from "next/link";

interface GroupActionsMenuProps {
  group: AdminGroup;
  onActionComplete: () => void;
}

type ActionType = "suspend" | "reactivate" | "archive" | null;

export function GroupActionsMenu({ group, onActionComplete }: GroupActionsMenuProps) {
  const [showMenu, setShowMenu] = useState(false);
  const [activeAction, setActiveAction] = useState<ActionType>(null);
  const [reason, setReason] = useState("");

  const { loading: suspendLoading, execute: executeSuspend } = useApi<AdminGroup>();
  const { loading: reactivateLoading, execute: executeReactivate } = useApi<AdminGroup>();
  const { loading: archiveLoading, execute: executeArchive } = useApi<AdminGroup>();

  const isLoading = suspendLoading || reactivateLoading || archiveLoading;

  const handleSuspend = async () => {
    await executeSuspend(() => adminGroupsApi.suspend(group.id, reason || undefined));
    setActiveAction(null);
    setReason("");
    onActionComplete();
  };

  const handleReactivate = async () => {
    await executeReactivate(() => adminGroupsApi.reactivate(group.id));
    setActiveAction(null);
    onActionComplete();
  };

  const handleArchive = async () => {
    await executeArchive(() => adminGroupsApi.archive(group.id));
    setActiveAction(null);
    onActionComplete();
  };

  return (
    <>
      <div className="relative">
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setShowMenu(!showMenu)}
          disabled={isLoading}
          aria-expanded={showMenu}
          aria-haspopup="menu"
          aria-label={`Actions for group ${group.name}`}
        >
          <MoreHorizontal className="h-4 w-4" />
        </Button>

        {showMenu && (
          <>
            <div
              className="fixed inset-0 z-10"
              onClick={() => setShowMenu(false)}
            />
            <div
              className="absolute right-0 mt-1 w-48 rounded-md border bg-background shadow-lg z-20"
              role="menu"
              aria-label="Group actions"
            >
              <div className="py-1">
                <Link
                  href={`/groups/${group.id}/members`}
                  className="flex items-center w-full px-4 py-2 text-sm hover:bg-muted"
                  role="menuitem"
                  onClick={() => setShowMenu(false)}
                >
                  <Users className="h-4 w-4 mr-2" aria-hidden="true" />
                  View Members
                </Link>
                <Link
                  href={`/groups/${group.id}/transfer`}
                  className="flex items-center w-full px-4 py-2 text-sm hover:bg-muted"
                  role="menuitem"
                  onClick={() => setShowMenu(false)}
                >
                  <UserCog className="h-4 w-4 mr-2" aria-hidden="true" />
                  Transfer Ownership
                </Link>
                {group.status === "active" && (
                  <button
                    className="flex items-center w-full px-4 py-2 text-sm hover:bg-muted"
                    onClick={() => {
                      setShowMenu(false);
                      setActiveAction("suspend");
                    }}
                    role="menuitem"
                  >
                    <Ban className="h-4 w-4 mr-2" aria-hidden="true" />
                    Suspend
                  </button>
                )}
                {group.status === "suspended" && (
                  <button
                    className="flex items-center w-full px-4 py-2 text-sm hover:bg-muted"
                    onClick={() => {
                      setShowMenu(false);
                      setActiveAction("reactivate");
                    }}
                    role="menuitem"
                  >
                    <RefreshCw className="h-4 w-4 mr-2" aria-hidden="true" />
                    Reactivate
                  </button>
                )}
                {group.status !== "archived" && (
                  <button
                    className="flex items-center w-full px-4 py-2 text-sm text-destructive hover:bg-muted"
                    onClick={() => {
                      setShowMenu(false);
                      setActiveAction("archive");
                    }}
                    role="menuitem"
                  >
                    <Archive className="h-4 w-4 mr-2" aria-hidden="true" />
                    Archive
                  </button>
                )}
              </div>
            </div>
          </>
        )}
      </div>

      {/* Confirmation Dialogs */}
      {activeAction && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          aria-labelledby="action-dialog-title"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setActiveAction(null)}
            aria-hidden="true"
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            {activeAction === "suspend" && (
              <>
                <div className="flex items-start gap-3 mb-4">
                  <Ban className="h-5 w-5 text-yellow-500 mt-0.5" aria-hidden="true" />
                  <div>
                    <h2 id="action-dialog-title" className="text-lg font-semibold">Suspend Group</h2>
                    <p className="text-sm text-muted-foreground">
                      Are you sure you want to suspend &ldquo;{group.name}&rdquo;? Members will not be able to sync data.
                    </p>
                  </div>
                </div>
                <div className="mb-4">
                  <label className="text-sm font-medium">Reason (optional)</label>
                  <input
                    type="text"
                    className="w-full mt-1 rounded-md border border-input bg-background px-3 py-2 text-sm"
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="Enter reason for suspension..."
                  />
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
                  <RefreshCw className="h-5 w-5 text-green-500 mt-0.5" aria-hidden="true" />
                  <div>
                    <h2 id="action-dialog-title" className="text-lg font-semibold">Reactivate Group</h2>
                    <p className="text-sm text-muted-foreground">
                      Are you sure you want to reactivate &ldquo;{group.name}&rdquo;? Members will be able to sync data again.
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

            {activeAction === "archive" && (
              <>
                <div className="flex items-start gap-3 mb-4">
                  <AlertTriangle className="h-5 w-5 text-destructive mt-0.5" aria-hidden="true" />
                  <div>
                    <h2 id="action-dialog-title" className="text-lg font-semibold text-destructive">Archive Group</h2>
                    <p className="text-sm text-muted-foreground">
                      Are you sure you want to archive &ldquo;{group.name}&rdquo;? This action makes the group read-only. Archived groups can be restored later.
                    </p>
                  </div>
                </div>
                <div className="flex justify-end gap-2">
                  <Button variant="outline" onClick={() => setActiveAction(null)} disabled={archiveLoading}>
                    Cancel
                  </Button>
                  <Button variant="destructive" onClick={handleArchive} disabled={archiveLoading}>
                    {archiveLoading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
                    Archive
                  </Button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
}
