"use client";

import { useState } from "react";
import type { Organization } from "@/types";
import { organizationsApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { OrganizationEditDialog } from "./organization-edit-dialog";
import { OrganizationSuspendDialog } from "./organization-suspend-dialog";
import { OrganizationLimitsDialog } from "./organization-limits-dialog";
import { OrganizationFeaturesDialog } from "./organization-features-dialog";
import { OrganizationStatsDialog } from "./organization-stats-dialog";
import {
  MoreHorizontal,
  Pencil,
  Ban,
  CheckCircle,
  Archive,
  Settings,
  ToggleLeft,
  BarChart3,
} from "lucide-react";

interface OrganizationActionsMenuProps {
  organization: Organization;
  onActionComplete: () => void;
}

type ActionType = "edit" | "suspend" | "limits" | "features" | "stats" | null;

export function OrganizationActionsMenu({ organization, onActionComplete }: OrganizationActionsMenuProps) {
  const [showMenu, setShowMenu] = useState(false);
  const [activeAction, setActiveAction] = useState<ActionType>(null);
  const [loading, setLoading] = useState(false);

  const handleReactivate = async () => {
    setLoading(true);
    try {
      const result = await organizationsApi.reactivate(organization.id);
      if (!result.error) {
        onActionComplete();
      }
    } finally {
      setLoading(false);
      setShowMenu(false);
    }
  };

  const handleArchive = async () => {
    setLoading(true);
    try {
      const result = await organizationsApi.archive(organization.id);
      if (!result.error) {
        onActionComplete();
      }
    } finally {
      setLoading(false);
      setShowMenu(false);
    }
  };

  const isSuspended = organization.status === "suspended";
  const isActive = organization.status === "active";
  const isArchived = organization.status === "archived";
  const isPending = organization.status === "pending";

  return (
    <div className="relative">
      <Button
        variant="ghost"
        size="sm"
        onClick={(e) => {
          e.stopPropagation();
          setShowMenu(!showMenu);
        }}
        aria-label="Organization actions"
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
            <button
              className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted"
              onClick={(e) => {
                e.stopPropagation();
                setActiveAction("edit");
                setShowMenu(false);
              }}
            >
              <Pencil className="h-4 w-4" />
              Edit Details
            </button>

            <button
              className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted"
              onClick={(e) => {
                e.stopPropagation();
                setActiveAction("limits");
                setShowMenu(false);
              }}
            >
              <Settings className="h-4 w-4" />
              Configure Limits
            </button>

            <button
              className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted"
              onClick={(e) => {
                e.stopPropagation();
                setActiveAction("features");
                setShowMenu(false);
              }}
            >
              <ToggleLeft className="h-4 w-4" />
              Manage Features
            </button>

            <button
              className="w-full flex items-center gap-2 px-3 py-2 text-sm hover:bg-muted"
              onClick={(e) => {
                e.stopPropagation();
                setActiveAction("stats");
                setShowMenu(false);
              }}
            >
              <BarChart3 className="h-4 w-4" />
              View Statistics
            </button>

            <div className="border-t my-1" />

            {(isActive || isPending) && (
              <button
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-destructive hover:bg-muted"
                onClick={(e) => {
                  e.stopPropagation();
                  setActiveAction("suspend");
                  setShowMenu(false);
                }}
              >
                <Ban className="h-4 w-4" />
                Suspend Organization
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
                <CheckCircle className="h-4 w-4" />
                {loading ? "Reactivating..." : "Reactivate"}
              </button>
            )}

            {!isArchived && (
              <button
                className="w-full flex items-center gap-2 px-3 py-2 text-sm text-muted-foreground hover:bg-muted"
                onClick={(e) => {
                  e.stopPropagation();
                  handleArchive();
                }}
                disabled={loading}
              >
                <Archive className="h-4 w-4" />
                {loading ? "Archiving..." : "Archive"}
              </button>
            )}
          </div>
        </>
      )}

      {/* Edit Dialog */}
      {activeAction === "edit" && (
        <OrganizationEditDialog
          organization={organization}
          onSuccess={() => {
            setActiveAction(null);
            onActionComplete();
          }}
          onCancel={() => setActiveAction(null)}
        />
      )}

      {/* Suspend Dialog */}
      {activeAction === "suspend" && (
        <OrganizationSuspendDialog
          organization={organization}
          onSuccess={() => {
            setActiveAction(null);
            onActionComplete();
          }}
          onCancel={() => setActiveAction(null)}
        />
      )}

      {/* Limits Dialog */}
      {activeAction === "limits" && (
        <OrganizationLimitsDialog
          organization={organization}
          onSuccess={() => {
            setActiveAction(null);
            onActionComplete();
          }}
          onCancel={() => setActiveAction(null)}
        />
      )}

      {/* Features Dialog */}
      {activeAction === "features" && (
        <OrganizationFeaturesDialog
          organization={organization}
          onSuccess={() => {
            setActiveAction(null);
            onActionComplete();
          }}
          onCancel={() => setActiveAction(null)}
        />
      )}

      {/* Stats Dialog */}
      {activeAction === "stats" && (
        <OrganizationStatsDialog
          organization={organization}
          onClose={() => setActiveAction(null)}
        />
      )}
    </div>
  );
}
