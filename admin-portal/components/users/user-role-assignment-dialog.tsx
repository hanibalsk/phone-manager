"use client";

import { useState, useEffect, useId, useCallback } from "react";
import type { AdminUser, UserRoleAssignment, Role, Organization } from "@/types";
import { rolesApi, organizationsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { X, Shield, RefreshCw, AlertCircle, Plus, Trash2, Building2 } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface UserRoleAssignmentDialogProps {
  user: AdminUser;
  onClose: () => void;
  onUpdate?: () => void;
}

export function UserRoleAssignmentDialog({ user, onClose, onUpdate }: UserRoleAssignmentDialogProps) {
  const [showAddForm, setShowAddForm] = useState(false);
  const [selectedRoleId, setSelectedRoleId] = useState("");
  const [selectedOrgId, setSelectedOrgId] = useState("");
  const [actionLoading, setActionLoading] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onClose });
  const titleId = useId();
  const descriptionId = useId();

  // Fetch user's role assignments
  const { data: assignments, loading: loadingAssignments, error: assignmentsError, execute: fetchAssignments } = useApi<UserRoleAssignment[]>();

  // Fetch available roles
  const { data: roles, loading: loadingRoles, execute: fetchRoles } = useApi<Role[]>();

  // Fetch organizations for the dropdown
  const { data: orgsResponse, loading: loadingOrgs, execute: fetchOrgs } = useApi<{ items: Organization[] }>();

  const organizations = orgsResponse?.items || [];

  const loadData = useCallback(() => {
    fetchAssignments(() => rolesApi.getUserAssignments(user.id));
    fetchRoles(() => rolesApi.list());
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchAssignments, fetchRoles, fetchOrgs, user.id]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleAssignRole = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedRoleId) {
      setError("Please select a role");
      return;
    }

    setActionLoading("assign");
    setError(null);
    try {
      const result = await rolesApi.assignRole(
        user.id,
        selectedRoleId,
        selectedOrgId || undefined
      );
      if (result.error) {
        setError(result.error);
      } else {
        setShowAddForm(false);
        setSelectedRoleId("");
        setSelectedOrgId("");
        loadData();
        onUpdate?.();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to assign role");
    } finally {
      setActionLoading(null);
    }
  };

  const handleRemoveRole = async (assignmentId: string) => {
    // Check if this is the last super_admin assignment
    const assignment = assignments?.find(a => a.id === assignmentId);
    if (assignment?.role_code === "super_admin") {
      const superAdminCount = assignments?.filter(a => a.role_code === "super_admin").length || 0;
      if (superAdminCount <= 1) {
        setError("Cannot remove the last super admin role");
        return;
      }
    }

    setActionLoading(assignmentId);
    setError(null);
    try {
      const result = await rolesApi.removeRole(user.id, assignmentId);
      if (result.error) {
        setError(result.error);
      } else {
        loadData();
        onUpdate?.();
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to remove role");
    } finally {
      setActionLoading(null);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const getRoleBadgeVariant = (code: string): "default" | "secondary" | "outline" => {
    switch (code) {
      case "super_admin":
        return "default";
      case "org_admin":
      case "org_manager":
        return "secondary";
      default:
        return "outline";
    }
  };

  const loading = loadingAssignments || loadingRoles || loadingOrgs;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-lg mx-4 max-h-[85vh] overflow-hidden flex flex-col">
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
            Role Assignments
          </CardTitle>
          <CardDescription id={descriptionId}>
            Manage roles for {user.display_name}
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-4 overflow-y-auto flex-1">
          {(error || assignmentsError) && (
            <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
              <AlertCircle className="h-4 w-4" />
              {error || assignmentsError}
            </div>
          )}

          {loading && !assignments && (
            <div className="flex items-center justify-center py-8">
              <RefreshCw className="h-6 w-6 animate-spin text-muted-foreground" />
            </div>
          )}

          {/* Current Role Assignments */}
          {assignments && assignments.length > 0 && (
            <div className="space-y-3">
              <Label>Current Roles</Label>
              <div className="space-y-2">
                {assignments.map((assignment) => (
                  <div
                    key={assignment.id}
                    className="flex items-center justify-between p-3 border rounded-lg"
                  >
                    <div className="flex items-center gap-3">
                      <div className="p-2 bg-primary/10 rounded">
                        <Shield className="h-4 w-4 text-primary" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-medium">{assignment.role_name}</span>
                          <Badge variant={getRoleBadgeVariant(assignment.role_code)}>
                            {assignment.role_code.replace(/_/g, " ").toUpperCase()}
                          </Badge>
                        </div>
                        <div className="flex items-center gap-2 text-sm text-muted-foreground">
                          {assignment.organization_name ? (
                            <>
                              <Building2 className="h-3 w-3" />
                              <span>{assignment.organization_name}</span>
                            </>
                          ) : (
                            <span>Global scope</span>
                          )}
                          <span>•</span>
                          <span>Assigned {formatDate(assignment.assigned_at)}</span>
                        </div>
                      </div>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-destructive hover:text-destructive hover:bg-destructive/10"
                      onClick={() => handleRemoveRole(assignment.id)}
                      disabled={actionLoading === assignment.id}
                      aria-label={`Remove ${assignment.role_name} role`}
                    >
                      {actionLoading === assignment.id ? (
                        <RefreshCw className="h-4 w-4 animate-spin" />
                      ) : (
                        <Trash2 className="h-4 w-4" />
                      )}
                    </Button>
                  </div>
                ))}
              </div>
            </div>
          )}

          {assignments && assignments.length === 0 && !loading && (
            <div className="text-center py-6 text-muted-foreground">
              <Shield className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p>No roles assigned</p>
            </div>
          )}

          {/* Add Role Form */}
          {showAddForm ? (
            <form onSubmit={handleAssignRole} className="space-y-4 p-4 border rounded-lg bg-muted/30">
              <div className="flex items-center justify-between">
                <Label className="text-base font-medium">Add New Role</Label>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  onClick={() => {
                    setShowAddForm(false);
                    setSelectedRoleId("");
                    setSelectedOrgId("");
                    setError(null);
                  }}
                >
                  Cancel
                </Button>
              </div>

              <div className="space-y-2">
                <Label htmlFor="role-select">
                  Role <span className="text-destructive">*</span>
                </Label>
                <select
                  id="role-select"
                  className="w-full h-10 px-3 border rounded-md bg-background text-sm"
                  value={selectedRoleId}
                  onChange={(e) => setSelectedRoleId(e.target.value)}
                  disabled={loadingRoles}
                >
                  <option value="">Select a role...</option>
                  {roles?.map((role) => (
                    <option key={role.id} value={role.id}>
                      {role.name} ({role.code.replace(/_/g, " ")})
                    </option>
                  ))}
                </select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="org-select">Organization (optional)</Label>
                <select
                  id="org-select"
                  className="w-full h-10 px-3 border rounded-md bg-background text-sm"
                  value={selectedOrgId}
                  onChange={(e) => setSelectedOrgId(e.target.value)}
                  disabled={loadingOrgs}
                >
                  <option value="">Global (all organizations)</option>
                  {organizations.map((org) => (
                    <option key={org.id} value={org.id}>
                      {org.name}
                    </option>
                  ))}
                </select>
                <p className="text-xs text-muted-foreground">
                  Leave empty for global access, or select an organization to scope the role.
                </p>
              </div>

              <Button
                type="submit"
                disabled={actionLoading === "assign" || !selectedRoleId}
                className="w-full"
              >
                {actionLoading === "assign" ? "Assigning..." : "Assign Role"}
              </Button>
            </form>
          ) : (
            <Button
              variant="outline"
              className="w-full"
              onClick={() => setShowAddForm(true)}
            >
              <Plus className="h-4 w-4 mr-2" />
              Add Role Assignment
            </Button>
          )}
        </CardContent>

        <CardFooter className="border-t">
          <Button variant="outline" onClick={onClose} className="ml-auto">
            Close
          </Button>
        </CardFooter>
      </Card>
    </div>
  );
}
