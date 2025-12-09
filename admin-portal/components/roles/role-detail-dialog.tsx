"use client";

import { useState, useId } from "react";
import type { Role, PermissionCategory } from "@/types";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { X, Shield, Check, Users, Pencil, Trash2 } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";
import { RoleEditDialog } from "./role-edit-dialog";
import { RoleDeleteDialog } from "./role-delete-dialog";

interface RoleDetailDialogProps {
  role: Role;
  onClose: () => void;
  onUpdate: () => void;
}

const CATEGORY_LABELS: Record<PermissionCategory, string> = {
  users: "Users",
  organizations: "Organizations",
  devices: "Devices",
  locations: "Locations",
  geofences: "Geofences",
  alerts: "Alerts",
  webhooks: "Webhooks",
  trips: "Trips",
  groups: "Groups",
  enrollment: "Enrollment",
  audit: "Audit",
  config: "Configuration",
  reports: "Reports",
  api_keys: "API Keys",
};

export function RoleDetailDialog({ role, onClose, onUpdate }: RoleDetailDialogProps) {
  const [showEditDialog, setShowEditDialog] = useState(false);
  const [showDeleteDialog, setShowDeleteDialog] = useState(false);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onClose });
  const titleId = useId();
  const descriptionId = useId();

  // Group permissions by category
  const permissionsByCategory = role.permissions.reduce((acc, perm) => {
    const category = perm.category;
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(perm);
    return acc;
  }, {} as Record<PermissionCategory, typeof role.permissions>);

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-2xl mx-4 max-h-[80vh] overflow-hidden flex flex-col">
        <CardHeader className="relative flex-shrink-0">
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="absolute right-4 top-4"
            onClick={onClose}
            aria-label="Close dialog"
          >
            <X className="h-4 w-4" aria-hidden="true" />
          </Button>
          <CardTitle id={titleId} className="flex items-center gap-2">
            <Shield className="h-5 w-5" aria-hidden="true" />
            {role.name}
          </CardTitle>
          <CardDescription id={descriptionId}>
            {role.description || "View role details and permissions"}
          </CardDescription>
        </CardHeader>

        <CardContent className="overflow-y-auto flex-1">
          {/* Role Info */}
          <div className="grid grid-cols-2 gap-4 mb-6">
            <div className="space-y-1">
              <p className="text-sm text-muted-foreground">Role Code</p>
              <Badge variant="secondary">{role.code.replace(/_/g, " ").toUpperCase()}</Badge>
            </div>
            <div className="space-y-1">
              <p className="text-sm text-muted-foreground">Type</p>
              <Badge variant={role.is_system ? "default" : "outline"}>
                {role.is_system ? "System Role" : "Custom Role"}
              </Badge>
            </div>
            <div className="space-y-1">
              <p className="text-sm text-muted-foreground">Users Assigned</p>
              <div className="flex items-center gap-1">
                <Users className="h-4 w-4 text-muted-foreground" />
                <span className="font-medium">{role.user_count}</span>
              </div>
            </div>
            <div className="space-y-1">
              <p className="text-sm text-muted-foreground">Created</p>
              <p className="text-sm">{formatDate(role.created_at)}</p>
            </div>
          </div>

          {/* Permissions */}
          <div className="space-y-4">
            <h4 className="font-medium flex items-center gap-2">
              Permissions
              <Badge variant="outline">{role.permissions.length} total</Badge>
            </h4>

            {Object.keys(permissionsByCategory).length === 0 ? (
              <p className="text-sm text-muted-foreground">No permissions assigned</p>
            ) : (
              <div className="space-y-4">
                {(Object.entries(permissionsByCategory) as [PermissionCategory, typeof role.permissions][]).map(
                  ([category, permissions]) => (
                    <div key={category} className="border rounded-lg p-4">
                      <h5 className="font-medium mb-3">{CATEGORY_LABELS[category] || category}</h5>
                      <div className="grid grid-cols-2 gap-2">
                        {permissions.map((perm) => (
                          <div
                            key={perm.id}
                            className="flex items-center gap-2 text-sm"
                            title={perm.description}
                          >
                            <Check className="h-4 w-4 text-green-500" />
                            <span>{perm.name}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )
                )}
              </div>
            )}
          </div>
        </CardContent>

        <div className="flex justify-between gap-2 p-6 border-t">
          <div>
            {!role.is_system && (
              <Button
                variant="destructive"
                size="sm"
                onClick={() => setShowDeleteDialog(true)}
              >
                <Trash2 className="h-4 w-4 mr-2" />
                Delete
              </Button>
            )}
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onClose}>
              Close
            </Button>
            {!role.is_system && (
              <Button onClick={() => setShowEditDialog(true)}>
                <Pencil className="h-4 w-4 mr-2" />
                Edit Role
              </Button>
            )}
          </div>
        </div>
      </Card>

      {/* Edit Dialog */}
      {showEditDialog && (
        <RoleEditDialog
          role={role}
          onSuccess={() => {
            setShowEditDialog(false);
            onUpdate();
            onClose();
          }}
          onCancel={() => setShowEditDialog(false)}
        />
      )}

      {/* Delete Dialog */}
      {showDeleteDialog && (
        <RoleDeleteDialog
          role={role}
          onSuccess={() => {
            setShowDeleteDialog(false);
            onUpdate();
            onClose();
          }}
          onCancel={() => setShowDeleteDialog(false)}
        />
      )}
    </div>
  );
}
