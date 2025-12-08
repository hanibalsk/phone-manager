"use client";

import { useState } from "react";
import type { AdminUser } from "@/types";
import { usersApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { UserSuspendDialog } from "./user-suspend-dialog";
import { UserResetPasswordDialog } from "./user-reset-password-dialog";
import { UserSessionsDialog } from "./user-sessions-dialog";
import { UserMfaDialog } from "./user-mfa-dialog";
import {
  MoreHorizontal,
  UserX,
  UserCheck,
  KeyRound,
  Shield,
  Monitor,
} from "lucide-react";

interface UserActionsMenuProps {
  user: AdminUser;
  onActionComplete: () => void;
}

type ActionType = "suspend" | "reset-password" | "sessions" | "mfa" | null;

export function UserActionsMenu({ user, onActionComplete }: UserActionsMenuProps) {
  const [showMenu, setShowMenu] = useState(false);
  const [activeAction, setActiveAction] = useState<ActionType>(null);
  const [loading, setLoading] = useState(false);

  const handleReactivate = async () => {
    setLoading(true);
    try {
      const result = await usersApi.reactivate(user.id);
      if (!result.error) {
        onActionComplete();
      }
    } finally {
      setLoading(false);
      setShowMenu(false);
    }
  };

  const isSuspended = user.status === "suspended";
  const isActive = user.status === "active";

  return (
    <div className="relative">
      <Button
        variant="ghost"
        size="sm"
        onClick={(e) => {
          e.stopPropagation();
          setShowMenu(!showMenu);
        }}
        aria-label="User actions"
      >
        <MoreHorizontal className="h-4 w-4" />
      </Button>

      {showMenu && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-40"
            onClick={() => setShowMenu(false)}
          />

          {/* Menu */}
          <div className="absolute right-0 top-full mt-1 z-50 w-48 bg-popover border rounded-md shadow-lg py-1">
            {isActive && (
              <button
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-destructive hover:bg-muted"
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveAction("suspend");
                  setShowMenu(false);
                }}
              >
                <UserX className="h-4 w-4" />
                Suspend User
              </button>
            )}

            {isSuspended && (
              <button
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-green-600 hover:bg-muted"
                onClick={(e) => {
                  e.stopPropagation();
                  handleReactivate();
                }}
                disabled={loading}
              >
                <UserCheck className="h-4 w-4" />
                {loading ? "Reactivating..." : "Reactivate User"}
              </button>
            )}

            <div className="border-t my-1" />

            <button
              className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted"
              onClick={(e) => {
                e.stopPropagation();
                setActiveAction("reset-password");
                setShowMenu(false);
              }}
            >
              <KeyRound className="h-4 w-4" />
              Reset Password
            </button>

            <button
              className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted"
              onClick={(e) => {
                e.stopPropagation();
                setActiveAction("sessions");
                setShowMenu(false);
              }}
            >
              <Monitor className="h-4 w-4" />
              Manage Sessions
            </button>

            <button
              className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted"
              onClick={(e) => {
                e.stopPropagation();
                setActiveAction("mfa");
                setShowMenu(false);
              }}
            >
              <Shield className="h-4 w-4" />
              Manage MFA
            </button>
          </div>
        </>
      )}

      {/* Suspend Dialog */}
      {activeAction === "suspend" && (
        <UserSuspendDialog
          user={user}
          onSuccess={() => {
            setActiveAction(null);
            onActionComplete();
          }}
          onCancel={() => setActiveAction(null)}
        />
      )}

      {/* Reset Password Dialog */}
      {activeAction === "reset-password" && (
        <UserResetPasswordDialog
          user={user}
          onSuccess={() => {
            setActiveAction(null);
            onActionComplete();
          }}
          onCancel={() => setActiveAction(null)}
        />
      )}

      {/* Sessions Dialog */}
      {activeAction === "sessions" && (
        <UserSessionsDialog
          user={user}
          onClose={() => setActiveAction(null)}
        />
      )}

      {/* MFA Dialog */}
      {activeAction === "mfa" && (
        <UserMfaDialog
          user={user}
          onClose={() => setActiveAction(null)}
        />
      )}
    </div>
  );
}
