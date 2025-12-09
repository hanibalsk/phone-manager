"use client";

import { useState, useEffect, useCallback } from "react";
import type { Role } from "@/types";
import { rolesApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Shield,
  RefreshCw,
  Plus,
  Users,
  Eye,
  ChevronRight,
} from "lucide-react";
import { RoleDetailDialog } from "./role-detail-dialog";
import { RoleCreateDialog } from "./role-create-dialog";

const ROLE_DESCRIPTIONS: Record<string, string> = {
  super_admin: "Full platform access with all permissions",
  org_admin: "Full access within assigned organization(s)",
  org_manager: "Limited management within assigned organization(s)",
  support: "Read-only access for customer support",
  viewer: "Dashboard and reports access only",
};

export function RoleList() {
  const [selectedRole, setSelectedRole] = useState<Role | null>(null);
  const [showCreateDialog, setShowCreateDialog] = useState(false);

  const { data: roles, loading, error, execute } = useApi<Role[]>();

  const fetchRoles = useCallback(() => {
    execute(() => rolesApi.list());
  }, [execute]);

  useEffect(() => {
    fetchRoles();
  }, [fetchRoles]);

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

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Roles & Permissions
            </CardTitle>
            <CardDescription>
              Manage system roles and their permissions
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={fetchRoles}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 mr-2 ${loading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
            <Button size="sm" onClick={() => setShowCreateDialog(true)}>
              <Plus className="h-4 w-4 mr-2" />
              Create Role
            </Button>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Error State */}
        {error && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <p className="text-destructive mb-4">{error}</p>
            <Button variant="outline" onClick={fetchRoles}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Retry
            </Button>
          </div>
        )}

        {/* Loading State */}
        {loading && !roles && (
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="flex items-center gap-4 p-4 border rounded-lg">
                <div className="h-10 w-10 bg-muted animate-pulse rounded" />
                <div className="flex-1 space-y-2">
                  <div className="h-4 w-32 bg-muted animate-pulse rounded" />
                  <div className="h-3 w-64 bg-muted animate-pulse rounded" />
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!loading && !error && roles?.length === 0 && (
          <div className="flex flex-col items-center justify-center py-8 text-center">
            <Shield className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No roles found</p>
          </div>
        )}

        {/* Role List */}
        {!error && roles && roles.length > 0 && (
          <div className="space-y-4">
            {roles.map((role) => (
              <div
                key={role.id}
                className="flex items-center justify-between p-4 border rounded-lg hover:bg-muted/50 cursor-pointer transition-colors"
                onClick={() => setSelectedRole(role)}
              >
                <div className="flex items-center gap-4">
                  <div className="p-2 bg-primary/10 rounded-lg">
                    <Shield className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-medium">{role.name}</h3>
                      <Badge variant={getRoleBadgeVariant(role.code)}>
                        {role.code.replace(/_/g, " ").toUpperCase()}
                      </Badge>
                      {role.is_system && (
                        <Badge variant="outline" className="text-xs">
                          System
                        </Badge>
                      )}
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {role.description || ROLE_DESCRIPTIONS[role.code] || "Custom role"}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-4">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Users className="h-4 w-4" />
                    <span>{role.user_count} users</span>
                  </div>
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Eye className="h-4 w-4" />
                    <span>{role.permissions.length} permissions</span>
                  </div>
                  <ChevronRight className="h-5 w-5 text-muted-foreground" />
                </div>
              </div>
            ))}
          </div>
        )}
      </CardContent>

      {/* Role Detail Dialog */}
      {selectedRole && (
        <RoleDetailDialog
          role={selectedRole}
          onClose={() => setSelectedRole(null)}
          onUpdate={fetchRoles}
        />
      )}

      {/* Create Role Dialog */}
      {showCreateDialog && (
        <RoleCreateDialog
          onSuccess={() => {
            setShowCreateDialog(false);
            fetchRoles();
          }}
          onCancel={() => setShowCreateDialog(false)}
        />
      )}
    </Card>
  );
}
