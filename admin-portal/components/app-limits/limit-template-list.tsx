"use client";

import { useState, useEffect, useCallback } from "react";
import type { LimitTemplate, Organization } from "@/types";
import { LimitTypeBadge } from "./limit-type-badge";
import { AppCategoryBadge } from "@/components/app-usage/app-category-badge";
import { appLimitsApi, organizationsApi } from "@/lib/api-client";
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
import Link from "next/link";
import {
  FileText,
  RefreshCw,
  Search,
  Plus,
  Trash2,
  Smartphone,
  Users,
  AlertTriangle,
  ChevronDown,
  ChevronUp,
} from "lucide-react";

const ITEMS_PER_PAGE = 20;

export function LimitTemplateList() {
  const [search, setSearch] = useState("");
  const [organizationId, setOrganizationId] = useState("");
  const [page, setPage] = useState(1);
  const [templateToDelete, setTemplateToDelete] = useState<LimitTemplate | null>(null);
  const [replacementTemplateId, setReplacementTemplateId] = useState("");
  const [expandedTemplates, setExpandedTemplates] = useState<Set<string>>(new Set());
  const [notification, setNotification] = useState<{ type: "success" | "error"; message: string } | null>(null);

  const { data: orgsData, execute: fetchOrgs } = useApi<{ items: Organization[] }>();
  const { data: templatesData, loading, error, execute: fetchTemplates } = useApi<{ items: LimitTemplate[]; total: number }>();
  const { execute: deleteTemplate } = useApi<void>();

  useEffect(() => {
    fetchOrgs(() => organizationsApi.list({ limit: 100 }));
  }, [fetchOrgs]);

  const loadTemplates = useCallback(() => {
    fetchTemplates(() =>
      appLimitsApi.listTemplates({
        organization_id: organizationId || undefined,
        page,
        limit: ITEMS_PER_PAGE,
      })
    );
  }, [fetchTemplates, organizationId, page]);

  useEffect(() => {
    loadTemplates();
  }, [loadTemplates]);

  // Clear notification after 3 seconds
  useEffect(() => {
    if (notification) {
      const timer = setTimeout(() => setNotification(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [notification]);

  const toggleExpand = (templateId: string) => {
    const newExpanded = new Set(expandedTemplates);
    if (newExpanded.has(templateId)) {
      newExpanded.delete(templateId);
    } else {
      newExpanded.add(templateId);
    }
    setExpandedTemplates(newExpanded);
  };

  const handleDelete = async () => {
    if (!templateToDelete) return;
    const result = await deleteTemplate(() =>
      appLimitsApi.deleteTemplate(templateToDelete.id, replacementTemplateId || undefined)
    );
    if (result !== undefined) {
      setNotification({ type: "success", message: "Template deleted successfully" });
      setTemplateToDelete(null);
      setReplacementTemplateId("");
      loadTemplates();
    } else {
      setNotification({ type: "error", message: "Failed to delete template" });
    }
  };

  const templates = templatesData?.items || [];
  const filteredTemplates = search
    ? templates.filter(
        (t) =>
          t.name.toLowerCase().includes(search.toLowerCase()) ||
          t.description?.toLowerCase().includes(search.toLowerCase())
      )
    : templates;
  const total = templatesData?.total || 0;
  const totalPages = Math.ceil(total / ITEMS_PER_PAGE);

  // Get other templates for replacement dropdown
  const otherTemplates = templates.filter((t) => t.id !== templateToDelete?.id);

  return (
    <Card data-testid="limit-templates-card">
      <CardHeader>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FileText className="h-6 w-6" />
            <div>
              <CardTitle>Limit Templates</CardTitle>
              <CardDescription>
                Create reusable limit configurations
              </CardDescription>
            </div>
          </div>
          <div className="flex gap-2">
            <Link href="/app-limits">
              <Button variant="outline" size="sm">
                Back to Limits
              </Button>
            </Link>
            <Button variant="outline" size="sm" onClick={loadTemplates}>
              <RefreshCw className="mr-2 h-4 w-4" />
              Refresh
            </Button>
            <Link href="/app-limits/templates/new">
              <Button size="sm">
                <Plus className="mr-2 h-4 w-4" />
                Create Template
              </Button>
            </Link>
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Notification */}
        {notification && (
          <div
            className={`mb-4 p-3 rounded-md ${
              notification.type === "success"
                ? "bg-green-100 text-green-800"
                : "bg-red-100 text-red-800"
            }`}
          >
            {notification.message}
          </div>
        )}

        {/* Filters */}
        <div className="flex flex-wrap gap-4 mb-6">
          <div className="flex-1 min-w-[200px]">
            <div className="relative">
              <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search templates..."
                className="pl-8"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                data-testid="templates-search"
              />
            </div>
          </div>
          <select
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
            value={organizationId}
            onChange={(e) => setOrganizationId(e.target.value)}
            data-testid="templates-org-filter"
          >
            <option value="">All Organizations</option>
            {orgsData?.items?.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
        </div>

        {/* Template List */}
        {loading ? (
          <div className="flex justify-center py-8">
            <div className="text-muted-foreground">Loading templates...</div>
          </div>
        ) : error ? (
          <div className="flex justify-center py-8 text-red-500">
            Error loading templates
          </div>
        ) : filteredTemplates.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <FileText className="h-12 w-12 mb-4 opacity-50" />
            <p>No limit templates found</p>
            <Link href="/app-limits/templates/new">
              <Button className="mt-4" size="sm">
                <Plus className="mr-2 h-4 w-4" />
                Create First Template
              </Button>
            </Link>
          </div>
        ) : (
          <>
            <div className="space-y-4">
              {filteredTemplates.map((template) => {
                const isExpanded = expandedTemplates.has(template.id);

                return (
                  <div key={template.id} className="border rounded-lg overflow-hidden" data-testid={`template-row-${template.id}`}>
                    <div
                      className="p-4 flex items-center justify-between cursor-pointer hover:bg-secondary/50"
                      onClick={() => toggleExpand(template.id)}
                    >
                      <div className="flex-1">
                        <div className="flex items-center gap-3">
                          <div className="font-medium">{template.name}</div>
                          <span className="text-xs text-muted-foreground bg-secondary px-2 py-0.5 rounded">
                            {template.rules.length} rule{template.rules.length !== 1 ? "s" : ""}
                          </span>
                        </div>
                        {template.description && (
                          <div className="text-sm text-muted-foreground mt-1">
                            {template.description}
                          </div>
                        )}
                        <div className="flex items-center gap-4 mt-2 text-sm text-muted-foreground">
                          <span>{template.organization_name}</span>
                          <span className="flex items-center gap-1">
                            <Smartphone className="h-3 w-3" />
                            {template.linked_device_count} device{template.linked_device_count !== 1 ? "s" : ""}
                          </span>
                          <span className="flex items-center gap-1">
                            <Users className="h-3 w-3" />
                            {template.linked_group_count} group{template.linked_group_count !== 1 ? "s" : ""}
                          </span>
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <Link
                          href={`/app-limits/templates/${template.id}/edit`}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <Button variant="outline" size="sm">
                            Edit
                          </Button>
                        </Link>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={(e) => {
                            e.stopPropagation();
                            setTemplateToDelete(template);
                          }}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                        {isExpanded ? (
                          <ChevronUp className="h-5 w-5 text-muted-foreground" />
                        ) : (
                          <ChevronDown className="h-5 w-5 text-muted-foreground" />
                        )}
                      </div>
                    </div>

                    {isExpanded && (
                      <div className="border-t bg-secondary/30 p-4">
                        <div className="text-sm font-medium mb-3">Rules</div>
                        <div className="space-y-2">
                          {template.rules.map((rule, index) => (
                            <div
                              key={index}
                              className="flex items-center justify-between p-3 bg-background rounded border"
                            >
                              <div className="flex items-center gap-3">
                                {rule.target_type === "category" ? (
                                  <AppCategoryBadge
                                    category={rule.target_value as "social" | "games" | "productivity" | "entertainment" | "education" | "communication" | "other"}
                                  />
                                ) : (
                                  <span className="text-sm">{rule.target_display}</span>
                                )}
                                <LimitTypeBadge type={rule.limit_type} />
                              </div>
                              <div className="text-sm text-muted-foreground">
                                {rule.limit_type === "blocked" ? (
                                  "Blocked"
                                ) : (
                                  <>
                                    {rule.daily_limit_minutes && `${rule.daily_limit_minutes}m/day`}
                                    {rule.daily_limit_minutes && rule.weekly_limit_minutes && " • "}
                                    {rule.weekly_limit_minutes && `${rule.weekly_limit_minutes}m/week`}
                                  </>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4">
                <div className="text-sm text-muted-foreground">
                  Showing {(page - 1) * ITEMS_PER_PAGE + 1} to{" "}
                  {Math.min(page * ITEMS_PER_PAGE, total)} of {total} templates
                </div>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === 1}
                    onClick={() => setPage(page - 1)}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={page === totalPages}
                    onClick={() => setPage(page + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            )}
          </>
        )}
      </CardContent>

      {/* Delete Confirmation Modal */}
      {templateToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center" role="dialog" aria-modal="true" data-testid="template-delete-dialog">
          <div className="absolute inset-0 bg-black/50" onClick={() => setTemplateToDelete(null)} />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="rounded-full bg-red-100 p-2">
                <AlertTriangle className="h-5 w-5 text-red-600" />
              </div>
              <h3 className="text-lg font-semibold">Delete Template</h3>
            </div>
            <p className="text-muted-foreground mb-4">
              Are you sure you want to delete &ldquo;{templateToDelete.name}&rdquo;?
            </p>

            {(templateToDelete.linked_device_count > 0 || templateToDelete.linked_group_count > 0) && (
              <div className="mb-4 p-3 rounded bg-yellow-50 text-yellow-800 text-sm">
                <p className="font-medium mb-2">
                  This template is linked to {templateToDelete.linked_device_count} device(s) and{" "}
                  {templateToDelete.linked_group_count} group(s).
                </p>
                <label className="text-sm font-medium">
                  Select replacement template (optional):
                </label>
                <select
                  className="mt-1 block w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={replacementTemplateId}
                  onChange={(e) => setReplacementTemplateId(e.target.value)}
                >
                  <option value="">No replacement (remove limits)</option>
                  {otherTemplates.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
                </select>
              </div>
            )}

            <div className="flex justify-end gap-3">
              <Button
                variant="outline"
                onClick={() => {
                  setTemplateToDelete(null);
                  setReplacementTemplateId("");
                }}
                data-testid="template-delete-cancel"
              >
                Cancel
              </Button>
              <Button variant="destructive" onClick={handleDelete} data-testid="template-delete-confirm">
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </Card>
  );
}
