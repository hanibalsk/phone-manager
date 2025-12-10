"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useApi } from "@/hooks/use-api";
import { featureFlagsApi } from "@/lib/api-client";
import type { FeatureFlag } from "@/types";
import {
  Loader2,
  Flag,
  AlertCircle,
  Check,
  Search,
  ToggleLeft,
  ToggleRight,
  AlertTriangle,
  Info,
} from "lucide-react";

// Category colors and labels
const categoryConfig: Record<
  string,
  { label: string; className: string; bgClass: string }
> = {
  core: {
    label: "Core",
    className: "text-blue-700",
    bgClass: "bg-blue-100",
  },
  tracking: {
    label: "Tracking",
    className: "text-green-700",
    bgClass: "bg-green-100",
  },
  communication: {
    label: "Communication",
    className: "text-purple-700",
    bgClass: "bg-purple-100",
  },
  analytics: {
    label: "Analytics",
    className: "text-orange-700",
    bgClass: "bg-orange-100",
  },
};

export function FeatureFlags() {
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // State
  const [features, setFeatures] = useState<FeatureFlag[]>([]);
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState<string>("");

  // Confirmation modal
  const [pendingToggle, setPendingToggle] = useState<{
    feature: FeatureFlag;
    newState: boolean;
  } | null>(null);

  // API hooks
  const { loading: loadingFeatures, execute: fetchFeatures } =
    useApi<FeatureFlag[]>();
  const { loading: togglingFeature, execute: toggleFeature } =
    useApi<FeatureFlag>();

  // Load features
  useEffect(() => {
    loadFeatures();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadFeatures = async () => {
    const result = await fetchFeatures(() => featureFlagsApi.list());
    if (result) {
      setFeatures(result);
    }
  };

  const handleToggleRequest = (feature: FeatureFlag) => {
    setPendingToggle({ feature, newState: !feature.enabled });
  };

  const confirmToggle = async () => {
    if (!pendingToggle) return;

    const result = await toggleFeature(() =>
      featureFlagsApi.toggle(pendingToggle.feature.id, pendingToggle.newState)
    );

    if (result) {
      setFeatures((prev) =>
        prev.map((f) => (f.id === result.id ? result : f))
      );
      setNotification({
        type: "success",
        message: `${pendingToggle.feature.name} ${
          pendingToggle.newState ? "enabled" : "disabled"
        }`,
      });
      setTimeout(() => setNotification(null), 3000);
    } else {
      setNotification({
        type: "error",
        message: "Failed to update feature flag",
      });
      setTimeout(() => setNotification(null), 3000);
    }

    setPendingToggle(null);
  };

  // Filter features
  const filteredFeatures = features.filter((feature) => {
    const matchesSearch =
      search === "" ||
      feature.name.toLowerCase().includes(search.toLowerCase()) ||
      feature.key.toLowerCase().includes(search.toLowerCase()) ||
      feature.description.toLowerCase().includes(search.toLowerCase());

    const matchesCategory =
      categoryFilter === "" || feature.category === categoryFilter;

    return matchesSearch && matchesCategory;
  });

  // Group features by category
  const groupedFeatures = filteredFeatures.reduce((acc, feature) => {
    const cat = feature.category;
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(feature);
    return acc;
  }, {} as Record<string, FeatureFlag[]>);

  // Get unique categories
  const categories = Array.from(new Set(features.map((f) => f.category)));

  // Check if a feature has dependencies that are disabled
  const hasDisabledDependencies = (feature: FeatureFlag): string[] => {
    if (!feature.dependencies) return [];
    return feature.dependencies.filter((depKey) => {
      const dep = features.find((f) => f.key === depKey);
      return dep && !dep.enabled;
    });
  };

  // Check if a feature has enabled dependents
  const hasEnabledDependents = (feature: FeatureFlag): string[] => {
    if (!feature.dependents) return [];
    return feature.dependents.filter((depKey) => {
      const dep = features.find((f) => f.key === depKey);
      return dep && dep.enabled;
    });
  };

  if (loadingFeatures && features.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="feature-flags-container">
      {/* Notification */}
      {notification && (
        <div
          className={`p-4 rounded-lg flex items-center gap-2 ${
            notification.type === "success"
              ? "bg-green-50 text-green-800 border border-green-200"
              : "bg-red-50 text-red-800 border border-red-200"
          }`}
        >
          {notification.type === "success" ? (
            <Check className="h-4 w-4" />
          ) : (
            <AlertCircle className="h-4 w-4" />
          )}
          {notification.message}
        </div>
      )}

      {/* Info Banner */}
      <div className="p-4 rounded-lg bg-blue-50 border border-blue-200 flex items-start gap-3">
        <Info className="h-5 w-5 text-blue-600 mt-0.5" />
        <div>
          <p className="text-blue-800 font-medium">Feature Flag Management</p>
          <p className="text-blue-700 text-sm">
            Enable or disable platform features. Changes take effect immediately.
            Some features have dependencies that must be enabled first.
          </p>
        </div>
      </div>

      {/* Filters */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search features..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-10"
            data-testid="feature-flags-search-input"
          />
        </div>
        <select
          value={categoryFilter}
          onChange={(e) => setCategoryFilter(e.target.value)}
          className="px-3 py-2 border rounded-md bg-background text-sm"
          data-testid="feature-flags-category-filter"
        >
          <option value="">All Categories</option>
          {categories.map((cat) => (
            <option key={cat} value={cat}>
              {categoryConfig[cat]?.label || cat}
            </option>
          ))}
        </select>
      </div>

      {/* Feature Summary */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-3 rounded-lg bg-green-50 border border-green-200" data-testid="feature-flags-enabled-count">
          <div className="text-2xl font-bold text-green-700">
            {features.filter((f) => f.enabled).length}
          </div>
          <div className="text-sm text-green-600">Enabled</div>
        </div>
        <div className="p-3 rounded-lg bg-gray-50 border border-gray-200" data-testid="feature-flags-disabled-count">
          <div className="text-2xl font-bold text-gray-700">
            {features.filter((f) => !f.enabled).length}
          </div>
          <div className="text-sm text-gray-600">Disabled</div>
        </div>
        <div className="p-3 rounded-lg bg-blue-50 border border-blue-200">
          <div className="text-2xl font-bold text-blue-700">
            {features.filter((f) => f.category === "core").length}
          </div>
          <div className="text-sm text-blue-600">Core Features</div>
        </div>
        <div className="p-3 rounded-lg bg-purple-50 border border-purple-200" data-testid="feature-flags-total-count">
          <div className="text-2xl font-bold text-purple-700">
            {features.length}
          </div>
          <div className="text-sm text-purple-600">Total Features</div>
        </div>
      </div>

      {/* Feature List by Category */}
      {Object.entries(groupedFeatures).map(([category, categoryFeatures]) => (
        <div key={category} className="space-y-3">
          <div className="flex items-center gap-2">
            <Flag
              className={`h-4 w-4 ${categoryConfig[category]?.className || "text-gray-700"}`}
            />
            <h3 className="font-semibold">
              {categoryConfig[category]?.label || category}
            </h3>
            <span className="text-sm text-muted-foreground">
              ({categoryFeatures.length})
            </span>
          </div>

          <div className="space-y-2">
            {categoryFeatures.map((feature) => {
              const disabledDeps = hasDisabledDependencies(feature);
              const enabledDependents = hasEnabledDependents(feature);

              return (
                <div
                  key={feature.id}
                  className={`p-4 rounded-lg border ${
                    feature.enabled
                      ? "border-green-200 bg-green-50/50"
                      : "border-border"
                  }`}
                  data-testid={`feature-flag-item-${feature.key}`}
                >
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium">{feature.name}</span>
                        <code className="text-xs px-1.5 py-0.5 rounded bg-muted">
                          {feature.key}
                        </code>
                      </div>
                      <p className="text-sm text-muted-foreground mt-1">
                        {feature.description}
                      </p>

                      {/* Dependencies warning */}
                      {disabledDeps.length > 0 && !feature.enabled && (
                        <div className="flex items-center gap-2 mt-2 text-amber-600 text-sm">
                          <AlertTriangle className="h-4 w-4" />
                          <span>
                            Requires:{" "}
                            {disabledDeps
                              .map((k) => features.find((f) => f.key === k)?.name || k)
                              .join(", ")}
                          </span>
                        </div>
                      )}

                      {/* Dependents warning when disabling */}
                      {enabledDependents.length > 0 && feature.enabled && (
                        <div className="flex items-center gap-2 mt-2 text-blue-600 text-sm">
                          <Info className="h-4 w-4" />
                          <span>
                            Required by:{" "}
                            {enabledDependents
                              .map((k) => features.find((f) => f.key === k)?.name || k)
                              .join(", ")}
                          </span>
                        </div>
                      )}
                    </div>

                    <button
                      onClick={() => handleToggleRequest(feature)}
                      disabled={togglingFeature}
                      className={`p-2 rounded-lg transition-colors ${
                        feature.enabled
                          ? "text-green-600 hover:bg-green-100"
                          : "text-muted-foreground hover:bg-muted"
                      }`}
                      data-testid={`feature-flag-toggle-${feature.key}`}
                    >
                      {feature.enabled ? (
                        <ToggleRight className="h-8 w-8" />
                      ) : (
                        <ToggleLeft className="h-8 w-8" />
                      )}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      ))}

      {/* Empty State */}
      {filteredFeatures.length === 0 && (
        <div className="text-center py-12 text-muted-foreground">
          <Flag className="h-12 w-12 mx-auto mb-4 opacity-50" />
          <p>No features found matching your criteria</p>
        </div>
      )}

      {/* Confirmation Modal */}
      {pendingToggle && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setPendingToggle(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6" data-testid="feature-flag-confirm-dialog">
            <div className="flex items-center gap-3 mb-4">
              <AlertTriangle className="h-6 w-6 text-amber-500" />
              <h3 className="text-lg font-semibold">Confirm Feature Change</h3>
            </div>

            <p className="text-muted-foreground mb-4">
              Are you sure you want to{" "}
              <strong>{pendingToggle.newState ? "enable" : "disable"}</strong>{" "}
              <strong>{pendingToggle.feature.name}</strong>?
            </p>

            {/* Warning for disabling with dependents */}
            {!pendingToggle.newState &&
              hasEnabledDependents(pendingToggle.feature).length > 0 && (
                <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 mb-4">
                  <div className="flex items-center gap-2 text-amber-700">
                    <AlertTriangle className="h-4 w-4" />
                    <span className="font-medium">Warning</span>
                  </div>
                  <p className="text-amber-600 text-sm mt-1">
                    This feature is required by other enabled features. Disabling
                    it may affect:{" "}
                    {hasEnabledDependents(pendingToggle.feature)
                      .map((k) => features.find((f) => f.key === k)?.name || k)
                      .join(", ")}
                  </p>
                </div>
              )}

            {/* Warning for enabling with disabled deps */}
            {pendingToggle.newState &&
              hasDisabledDependencies(pendingToggle.feature).length > 0 && (
                <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 mb-4">
                  <div className="flex items-center gap-2 text-amber-700">
                    <AlertTriangle className="h-4 w-4" />
                    <span className="font-medium">Dependencies Required</span>
                  </div>
                  <p className="text-amber-600 text-sm mt-1">
                    This feature requires other features to be enabled first:{" "}
                    {hasDisabledDependencies(pendingToggle.feature)
                      .map((k) => features.find((f) => f.key === k)?.name || k)
                      .join(", ")}
                  </p>
                </div>
              )}

            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setPendingToggle(null)} data-testid="feature-flag-cancel">
                Cancel
              </Button>
              <Button
                onClick={confirmToggle}
                disabled={togglingFeature}
                variant={pendingToggle.newState ? "default" : "destructive"}
                data-testid="feature-flag-confirm"
              >
                {togglingFeature ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : null}
                {pendingToggle.newState ? "Enable" : "Disable"}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
