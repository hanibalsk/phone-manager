"use client";

import { useState, useEffect, useId, useCallback } from "react";
import type { Role, Permission, PermissionCategory, UpdateRoleRequest } from "@/types";
import { rolesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { X, Shield, AlertCircle, RefreshCw } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface RoleEditDialogProps {
  role: Role;
  onSuccess: () => void;
  onCancel: () => void;
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

interface FormErrors {
  name?: string;
  permissions?: string;
  general?: string;
}

export function RoleEditDialog({ role, onSuccess, onCancel }: RoleEditDialogProps) {
  const [name, setName] = useState(role.name);
  const [description, setDescription] = useState(role.description);
  const [selectedPermissions, setSelectedPermissions] = useState<Set<string>>(
    new Set(role.permissions.map(p => p.id))
  );
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  // Fetch available permissions
  const { data: permissions, loading: loadingPermissions, error: permissionsError, execute: fetchPermissions } = useApi<Permission[]>();

  useEffect(() => {
    fetchPermissions(() => rolesApi.listPermissions());
  }, [fetchPermissions]);

  // Group permissions by category
  const permissionsByCategory = (permissions || []).reduce((acc, perm) => {
    const category = perm.category;
    if (!acc[category]) {
      acc[category] = [];
    }
    acc[category].push(perm);
    return acc;
  }, {} as Record<PermissionCategory, Permission[]>);

  const handlePermissionToggle = (permissionId: string) => {
    setSelectedPermissions((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(permissionId)) {
        newSet.delete(permissionId);
      } else {
        newSet.add(permissionId);
      }
      return newSet;
    });
    if (errors.permissions) {
      setErrors({ ...errors, permissions: undefined });
    }
  };

  const handleCategoryToggle = (categoryPermissions: Permission[]) => {
    const allSelected = categoryPermissions.every((p) => selectedPermissions.has(p.id));
    setSelectedPermissions((prev) => {
      const newSet = new Set(prev);
      if (allSelected) {
        categoryPermissions.forEach((p) => newSet.delete(p.id));
      } else {
        categoryPermissions.forEach((p) => newSet.add(p.id));
      }
      return newSet;
    });
  };

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    if (!name.trim()) {
      newErrors.name = "Role name is required";
    }

    if (selectedPermissions.size === 0) {
      newErrors.permissions = "At least one permission is required";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    setErrors({});

    try {
      const data: UpdateRoleRequest = {
        name: name.trim(),
        description: description.trim(),
        permission_ids: Array.from(selectedPermissions),
      };

      const result = await rolesApi.update(role.id, data);

      if (result.error) {
        setErrors({ general: result.error });
        return;
      }

      onSuccess();
    } catch (error) {
      setErrors({
        general: error instanceof Error ? error.message : "Failed to update role"
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-2xl mx-4 max-h-[85vh] overflow-hidden flex flex-col">
        <form onSubmit={handleSubmit} className="flex flex-col h-full">
          <CardHeader className="relative flex-shrink-0">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="absolute right-4 top-4"
              onClick={onCancel}
              aria-label="Close dialog"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </Button>
            <CardTitle id={titleId} className="flex items-center gap-2">
              <Shield className="h-5 w-5" aria-hidden="true" />
              Edit Role
            </CardTitle>
            <CardDescription id={descriptionId}>
              Modify the role name, description, and permissions
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4 overflow-y-auto flex-1">
            {errors.general && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {errors.general}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="name">
                Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="name"
                placeholder="Custom Manager"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (errors.name) setErrors({ ...errors, name: undefined });
                }}
                className={errors.name ? "border-destructive" : ""}
              />
              {errors.name && (
                <p className="text-sm text-destructive">{errors.name}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="code">Code</Label>
              <Input
                id="code"
                value={role.code}
                disabled
                className="bg-muted"
              />
              <p className="text-xs text-muted-foreground">
                Role code cannot be changed after creation.
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                placeholder="Describe what this role is for..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
              />
            </div>

            {/* Permissions Section */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <Label>
                  Permissions <span className="text-destructive">*</span>
                </Label>
                <span className="text-sm text-muted-foreground">
                  {selectedPermissions.size} selected
                </span>
              </div>

              {errors.permissions && (
                <p className="text-sm text-destructive">{errors.permissions}</p>
              )}

              {loadingPermissions && (
                <div className="flex items-center justify-center py-8">
                  <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
                </div>
              )}

              {permissionsError && (
                <div className="flex flex-col items-center gap-2 py-4">
                  <p className="text-sm text-destructive">{permissionsError}</p>
                  <Button variant="outline" size="sm" onClick={() => fetchPermissions(() => rolesApi.listPermissions())}>
                    Retry
                  </Button>
                </div>
              )}

              {!loadingPermissions && !permissionsError && permissions && (
                <div className="space-y-4 max-h-[300px] overflow-y-auto border rounded-lg p-4">
                  {(Object.entries(permissionsByCategory) as [PermissionCategory, Permission[]][]).map(
                    ([category, categoryPermissions]) => {
                      const allSelected = categoryPermissions.every((p) => selectedPermissions.has(p.id));
                      const someSelected = categoryPermissions.some((p) => selectedPermissions.has(p.id));

                      return (
                        <div key={category} className="space-y-2">
                          <div className="flex items-center gap-2">
                            <Checkbox
                              id={`category-${category}`}
                              checked={allSelected}
                              onCheckedChange={() => handleCategoryToggle(categoryPermissions)}
                              className={someSelected && !allSelected ? "opacity-50" : ""}
                            />
                            <Label htmlFor={`category-${category}`} className="font-medium cursor-pointer">
                              {CATEGORY_LABELS[category] || category}
                            </Label>
                          </div>
                          <div className="grid grid-cols-2 gap-2 ml-6">
                            {categoryPermissions.map((perm) => (
                              <div key={perm.id} className="flex items-center gap-2">
                                <Checkbox
                                  id={`edit-${perm.id}`}
                                  checked={selectedPermissions.has(perm.id)}
                                  onCheckedChange={() => handlePermissionToggle(perm.id)}
                                />
                                <Label
                                  htmlFor={`edit-${perm.id}`}
                                  className="text-sm cursor-pointer"
                                  title={perm.description}
                                >
                                  {perm.name}
                                </Label>
                              </div>
                            ))}
                          </div>
                        </div>
                      );
                    }
                  )}
                </div>
              )}
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-2 border-t">
            <Button
              type="button"
              variant="outline"
              onClick={onCancel}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={submitting || loadingPermissions}>
              {submitting ? "Saving..." : "Save Changes"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
