"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useApi } from "@/hooks/use-api";
import { apiKeysApi } from "@/lib/api-client";
import type {
  ApiKey,
  CreateApiKeyRequest,
  CreateApiKeyResponse,
  ApiKeyUsageStats,
  ApiKeyPermissionSet,
  ApiKeyScope,
  ApiKeyPermission,
} from "@/types";
import {
  Loader2,
  Key,
  AlertCircle,
  Check,
  Plus,
  Trash2,
  Edit,
  Save,
  Copy,
  Eye,
  EyeOff,
  RefreshCw,
  BarChart3,
  Calendar,
  Shield,
  ToggleLeft,
  ToggleRight,
  X,
} from "lucide-react";

const scopeLabels: Record<ApiKeyScope, string> = {
  devices: "Devices",
  locations: "Locations",
  users: "Users",
  organizations: "Organizations",
  webhooks: "Webhooks",
  all: "All Resources",
};

const permissionLabels: Record<ApiKeyPermission, string> = {
  read: "Read",
  write: "Write",
  admin: "Admin",
};

export function ApiKeys() {
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // State
  const [keys, setKeys] = useState<ApiKey[]>([]);
  const [selectedKey, setSelectedKey] = useState<ApiKey | null>(null);
  const [keyUsage, setKeyUsage] = useState<ApiKeyUsageStats | null>(null);

  // Form state
  const [showForm, setShowForm] = useState(false);
  const [editingKey, setEditingKey] = useState<ApiKey | null>(null);
  const [formData, setFormData] = useState<CreateApiKeyRequest>({
    name: "",
    description: "",
    permissions: [],
    rate_limit_per_minute: undefined,
    expires_at: undefined,
  });

  // New key display
  const [newKeySecret, setNewKeySecret] = useState<string | null>(null);
  const [showSecret, setShowSecret] = useState(false);
  const [copiedSecret, setCopiedSecret] = useState(false);

  // Delete and rotate confirmations
  const [keyToDelete, setKeyToDelete] = useState<ApiKey | null>(null);
  const [keyToRotate, setKeyToRotate] = useState<ApiKey | null>(null);
  const [rotateGracePeriod, setRotateGracePeriod] = useState<number>(0);

  // API hooks
  const { loading: loadingKeys, execute: fetchKeys } = useApi<ApiKey[]>();
  const { loading: savingKey, execute: saveKey } = useApi<CreateApiKeyResponse | ApiKey>();
  const { loading: deletingKey, execute: deleteKey } = useApi<void>();
  const { loading: loadingUsage, execute: fetchUsage } = useApi<ApiKeyUsageStats>();
  const { loading: rotatingKey, execute: rotateKey } = useApi<CreateApiKeyResponse>();
  const { loading: togglingKey, execute: toggleKey } = useApi<ApiKey>();

  // Load keys
  useEffect(() => {
    loadKeys();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadKeys = async () => {
    const result = await fetchKeys(() => apiKeysApi.list());
    if (result) {
      setKeys(result);
    }
  };

  const handleSelectKey = async (key: ApiKey) => {
    setSelectedKey(key);
    const usage = await fetchUsage(() => apiKeysApi.getUsage(key.id));
    if (usage) {
      setKeyUsage(usage);
    }
  };

  const startEditing = (key: ApiKey) => {
    setEditingKey(key);
    setFormData({
      name: key.name,
      description: key.description || "",
      permissions: key.permissions,
      rate_limit_per_minute: key.rate_limit_per_minute || undefined,
      expires_at: key.expires_at || undefined,
    });
    setShowForm(true);
  };

  const startCreating = () => {
    setEditingKey(null);
    setFormData({
      name: "",
      description: "",
      permissions: [],
      rate_limit_per_minute: undefined,
      expires_at: undefined,
    });
    setShowForm(true);
  };

  const handleSaveKey = async () => {
    if (editingKey) {
      const result = await saveKey(() =>
        apiKeysApi.update(editingKey.id, formData)
      );
      if (result && "id" in result) {
        setKeys((prev) =>
          prev.map((k) => (k.id === (result as ApiKey).id ? (result as ApiKey) : k))
        );
        setNotification({ type: "success", message: "API key updated" });
      }
    } else {
      const result = await saveKey(() => apiKeysApi.create(formData));
      if (result && "secret_key" in result) {
        const response = result as CreateApiKeyResponse;
        setKeys((prev) => [...prev, response.api_key]);
        setNewKeySecret(response.secret_key);
        setNotification({
          type: "success",
          message: "API key created! Copy the secret key now - it won't be shown again.",
        });
      }
    }

    setShowForm(false);
    setEditingKey(null);
    setTimeout(() => setNotification(null), 5000);
  };

  const handleDeleteKey = async () => {
    if (!keyToDelete) return;

    await deleteKey(() => apiKeysApi.delete(keyToDelete.id));
    setKeys((prev) => prev.filter((k) => k.id !== keyToDelete.id));
    if (selectedKey?.id === keyToDelete.id) {
      setSelectedKey(null);
      setKeyUsage(null);
    }
    setKeyToDelete(null);
    setNotification({ type: "success", message: "API key deleted" });
    setTimeout(() => setNotification(null), 3000);
  };

  const handleRotateKey = async () => {
    if (!keyToRotate) return;

    const result = await rotateKey(() =>
      apiKeysApi.rotate(keyToRotate.id, rotateGracePeriod > 0 ? rotateGracePeriod : undefined)
    );

    if (result) {
      setKeys((prev) =>
        prev.map((k) => (k.id === result.api_key.id ? result.api_key : k))
      );
      setNewKeySecret(result.secret_key);
      setKeyToRotate(null);
      setNotification({
        type: "success",
        message: "API key rotated! Copy the new secret key now.",
      });
      setTimeout(() => setNotification(null), 5000);
    }
  };

  const handleToggleKey = async (key: ApiKey) => {
    const result = await toggleKey(() =>
      apiKeysApi.toggleActive(key.id, !key.is_active)
    );
    if (result) {
      setKeys((prev) =>
        prev.map((k) => (k.id === result.id ? result : k))
      );
      setNotification({
        type: "success",
        message: `API key ${result.is_active ? "activated" : "deactivated"}`,
      });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  const copySecret = () => {
    if (newKeySecret) {
      navigator.clipboard.writeText(newKeySecret);
      setCopiedSecret(true);
      setTimeout(() => setCopiedSecret(false), 2000);
    }
  };

  const togglePermission = (scope: ApiKeyScope, permission: ApiKeyPermission) => {
    const existing = formData.permissions.find(
      (p) => p.scope === scope && p.permission === permission
    );

    if (existing) {
      setFormData((prev) => ({
        ...prev,
        permissions: prev.permissions.filter(
          (p) => !(p.scope === scope && p.permission === permission)
        ),
      }));
    } else {
      setFormData((prev) => ({
        ...prev,
        permissions: [...prev.permissions, { scope, permission }],
      }));
    }
  };

  const hasPermission = (scope: ApiKeyScope, permission: ApiKeyPermission) => {
    return formData.permissions.some(
      (p) => p.scope === scope && p.permission === permission
    );
  };

  if (loadingKeys && keys.length === 0) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6" data-testid="api-keys-container">
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

      {/* New Key Secret Display */}
      {newKeySecret && (
        <div className="p-4 rounded-lg bg-amber-50 border border-amber-200" data-testid="api-key-secret-display">
          <div className="flex items-center gap-2 mb-2">
            <AlertCircle className="h-5 w-5 text-amber-600" />
            <span className="font-semibold text-amber-800">
              Save your API key secret now!
            </span>
          </div>
          <p className="text-sm text-amber-700 mb-3">
            This is the only time you&apos;ll see this key. Store it securely.
          </p>
          <div className="flex items-center gap-2">
            <code className="flex-1 p-3 bg-white rounded border font-mono text-sm break-all">
              {showSecret ? newKeySecret : "•".repeat(40)}
            </code>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowSecret(!showSecret)}
              data-testid="api-key-toggle-secret"
            >
              {showSecret ? (
                <EyeOff className="h-4 w-4" />
              ) : (
                <Eye className="h-4 w-4" />
              )}
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={copySecret}
              className={copiedSecret ? "text-green-600" : ""}
              data-testid="api-key-copy-secret"
            >
              {copiedSecret ? (
                <Check className="h-4 w-4" />
              ) : (
                <Copy className="h-4 w-4" />
              )}
            </Button>
          </div>
          <Button
            variant="outline"
            size="sm"
            className="mt-3"
            onClick={() => setNewKeySecret(null)}
          >
            I&apos;ve saved the key
          </Button>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          Manage API keys for system integrations and external access.
        </p>
        <Button onClick={startCreating} data-testid="api-keys-create-button">
          <Plus className="h-4 w-4 mr-2" />
          Create API Key
        </Button>
      </div>

      {/* Keys List and Detail View */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Keys List */}
        <div className="lg:col-span-2 space-y-3">
          {keys.length === 0 ? (
            <div className="text-center py-12 text-muted-foreground border rounded-lg">
              <Key className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>No API keys configured</p>
              <p className="text-sm">Create your first API key to get started</p>
            </div>
          ) : (
            keys.map((key) => (
              <div
                key={key.id}
                onClick={() => handleSelectKey(key)}
                className={`p-4 rounded-lg border cursor-pointer transition-colors ${
                  selectedKey?.id === key.id
                    ? "border-primary bg-primary/5"
                    : key.is_active
                    ? "border-border hover:border-primary/50"
                    : "border-amber-200 bg-amber-50/50"
                }`}
                data-testid={`api-key-item-${key.id}`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <Key className="h-4 w-4 text-muted-foreground" />
                      <span className="font-medium">{key.name}</span>
                      {!key.is_active && (
                        <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">
                          Inactive
                        </span>
                      )}
                      {key.expires_at &&
                        new Date(key.expires_at) < new Date() && (
                          <span className="text-xs px-2 py-0.5 rounded-full bg-red-100 text-red-700">
                            Expired
                          </span>
                        )}
                    </div>
                    {key.description && (
                      <p className="text-sm text-muted-foreground mt-1">
                        {key.description}
                      </p>
                    )}
                    <div className="flex items-center gap-4 mt-2 text-xs text-muted-foreground">
                      <code className="px-1.5 py-0.5 rounded bg-muted">
                        {key.key_prefix}...
                      </code>
                      <span>{key.permissions.length} permissions</span>
                      <span>{key.total_requests.toLocaleString()} requests</span>
                    </div>
                  </div>
                  <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                    <button
                      onClick={() => handleToggleKey(key)}
                      disabled={togglingKey}
                      className={`p-2 rounded transition-colors ${
                        key.is_active
                          ? "text-green-600 hover:bg-green-50"
                          : "text-muted-foreground hover:bg-muted"
                      }`}
                      data-testid={`api-key-toggle-${key.id}`}
                    >
                      {key.is_active ? (
                        <ToggleRight className="h-5 w-5" />
                      ) : (
                        <ToggleLeft className="h-5 w-5" />
                      )}
                    </button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => startEditing(key)}
                      data-testid={`api-key-edit-${key.id}`}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setKeyToRotate(key)}
                      data-testid={`api-key-rotate-${key.id}`}
                    >
                      <RefreshCw className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                      onClick={() => setKeyToDelete(key)}
                      data-testid={`api-key-delete-${key.id}`}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Usage Stats Panel */}
        <div className="lg:col-span-1">
          {selectedKey ? (
            <div className="p-4 rounded-lg border space-y-4" data-testid="api-key-usage-stats">
              <div className="flex items-center justify-between">
                <h3 className="font-semibold flex items-center gap-2">
                  <BarChart3 className="h-4 w-4" />
                  Usage Statistics
                </h3>
                {loadingUsage && (
                  <Loader2 className="h-4 w-4 animate-spin" />
                )}
              </div>

              {keyUsage && (
                <>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="p-3 rounded-lg bg-muted/50">
                      <p className="text-2xl font-bold">
                        {keyUsage.total_requests.toLocaleString()}
                      </p>
                      <p className="text-xs text-muted-foreground">Total Requests</p>
                    </div>
                    <div className="p-3 rounded-lg bg-muted/50">
                      <p className="text-2xl font-bold">
                        {keyUsage.requests_last_24h.toLocaleString()}
                      </p>
                      <p className="text-xs text-muted-foreground">Last 24h</p>
                    </div>
                    <div className="p-3 rounded-lg bg-muted/50">
                      <p className="text-2xl font-bold">
                        {keyUsage.requests_last_7d.toLocaleString()}
                      </p>
                      <p className="text-xs text-muted-foreground">Last 7 Days</p>
                    </div>
                    <div className="p-3 rounded-lg bg-muted/50">
                      <p className="text-2xl font-bold text-red-600">
                        {keyUsage.error_count_last_24h}
                      </p>
                      <p className="text-xs text-muted-foreground">Errors (24h)</p>
                    </div>
                  </div>

                  {keyUsage.last_used_at && (
                    <p className="text-sm text-muted-foreground">
                      Last used:{" "}
                      {new Date(keyUsage.last_used_at).toLocaleString()}
                    </p>
                  )}

                  {keyUsage.most_used_endpoints.length > 0 && (
                    <div>
                      <p className="text-sm font-medium mb-2">Top Endpoints</p>
                      <div className="space-y-1">
                        {keyUsage.most_used_endpoints.slice(0, 5).map((ep, i) => (
                          <div
                            key={i}
                            className="flex items-center justify-between text-sm"
                          >
                            <code className="text-xs truncate max-w-[150px]">
                              {ep.endpoint}
                            </code>
                            <span className="text-muted-foreground">
                              {ep.count.toLocaleString()}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  )}
                </>
              )}

              {/* Key Details */}
              <div className="pt-4 border-t">
                <p className="text-sm font-medium mb-2">Permissions</p>
                <div className="flex flex-wrap gap-1">
                  {selectedKey.permissions.map((p, i) => (
                    <span
                      key={i}
                      className="text-xs px-2 py-1 rounded bg-muted"
                    >
                      {scopeLabels[p.scope]}: {permissionLabels[p.permission]}
                    </span>
                  ))}
                </div>

                {selectedKey.expires_at && (
                  <div className="mt-3 flex items-center gap-2 text-sm">
                    <Calendar className="h-4 w-4 text-muted-foreground" />
                    <span>
                      Expires:{" "}
                      {new Date(selectedKey.expires_at).toLocaleDateString()}
                    </span>
                  </div>
                )}

                {selectedKey.rate_limit_per_minute && (
                  <div className="mt-2 flex items-center gap-2 text-sm">
                    <Shield className="h-4 w-4 text-muted-foreground" />
                    <span>
                      Rate limit: {selectedKey.rate_limit_per_minute}/min
                    </span>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="p-4 rounded-lg border text-center text-muted-foreground">
              <BarChart3 className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p>Select an API key to view usage statistics</p>
            </div>
          )}
        </div>
      </div>

      {/* Create/Edit Form Modal */}
      {showForm && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setShowForm(false)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-2xl mx-4 p-6 max-h-[90vh] overflow-y-auto" data-testid="api-key-form-dialog">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">
                {editingKey ? "Edit API Key" : "Create API Key"}
              </h3>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowForm(false)}
              >
                <X className="h-4 w-4" />
              </Button>
            </div>

            <div className="space-y-4">
              <div className="space-y-2">
                <Label>Name</Label>
                <Input
                  value={formData.name}
                  onChange={(e) =>
                    setFormData((prev) => ({ ...prev, name: e.target.value }))
                  }
                  placeholder="My API Key"
                  data-testid="api-key-name-input"
                />
              </div>

              <div className="space-y-2">
                <Label>Description</Label>
                <Input
                  value={formData.description || ""}
                  onChange={(e) =>
                    setFormData((prev) => ({
                      ...prev,
                      description: e.target.value,
                    }))
                  }
                  placeholder="What is this key used for?"
                  data-testid="api-key-description-input"
                />
              </div>

              {/* Permissions Matrix */}
              <div className="space-y-2">
                <Label>Permissions</Label>
                <div className="border rounded-lg overflow-hidden">
                  <table className="w-full text-sm">
                    <thead className="bg-muted">
                      <tr>
                        <th className="p-2 text-left font-medium">Scope</th>
                        {(["read", "write", "admin"] as ApiKeyPermission[]).map(
                          (perm) => (
                            <th key={perm} className="p-2 text-center font-medium">
                              {permissionLabels[perm]}
                            </th>
                          )
                        )}
                      </tr>
                    </thead>
                    <tbody>
                      {(Object.keys(scopeLabels) as ApiKeyScope[]).map((scope) => (
                        <tr key={scope} className="border-t">
                          <td className="p-2">{scopeLabels[scope]}</td>
                          {(["read", "write", "admin"] as ApiKeyPermission[]).map(
                            (perm) => (
                              <td key={perm} className="p-2 text-center">
                                <input
                                  type="checkbox"
                                  checked={hasPermission(scope, perm)}
                                  onChange={() => togglePermission(scope, perm)}
                                  className="h-4 w-4 rounded border-gray-300"
                                  data-testid={`api-key-permission-${scope}-${perm}`}
                                />
                              </td>
                            )
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label>Rate Limit (per minute)</Label>
                  <Input
                    type="number"
                    value={formData.rate_limit_per_minute || ""}
                    onChange={(e) =>
                      setFormData((prev) => ({
                        ...prev,
                        rate_limit_per_minute: e.target.value
                          ? parseInt(e.target.value, 10)
                          : undefined,
                      }))
                    }
                    placeholder="Leave blank for no limit"
                    data-testid="api-key-rate-limit-input"
                  />
                </div>

                <div className="space-y-2">
                  <Label>Expiration Date</Label>
                  <Input
                    type="date"
                    value={
                      formData.expires_at
                        ? new Date(formData.expires_at).toISOString().split("T")[0]
                        : ""
                    }
                    onChange={(e) =>
                      setFormData((prev) => ({
                        ...prev,
                        expires_at: e.target.value || undefined,
                      }))
                    }
                    data-testid="api-key-expiration-input"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <Button variant="outline" onClick={() => setShowForm(false)} data-testid="api-key-form-cancel">
                Cancel
              </Button>
              <Button
                onClick={handleSaveKey}
                disabled={
                  savingKey ||
                  !formData.name ||
                  formData.permissions.length === 0
                }
                data-testid="api-key-form-save"
              >
                {savingKey ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <Save className="h-4 w-4 mr-2" />
                )}
                {editingKey ? "Update" : "Create"}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {keyToDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setKeyToDelete(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6" data-testid="api-key-delete-dialog">
            <div className="flex items-center gap-3 mb-4">
              <AlertCircle className="h-6 w-6 text-red-500" />
              <h3 className="text-lg font-semibold">Delete API Key</h3>
            </div>
            <p className="text-muted-foreground mb-4">
              Are you sure you want to delete <strong>{keyToDelete.name}</strong>?
              This action cannot be undone and any integrations using this key will
              stop working immediately.
            </p>
            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setKeyToDelete(null)} data-testid="api-key-delete-cancel">
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={handleDeleteKey}
                disabled={deletingKey}
                data-testid="api-key-delete-confirm"
              >
                {deletingKey ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <Trash2 className="h-4 w-4 mr-2" />
                )}
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Rotate Confirmation Modal */}
      {keyToRotate && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setKeyToRotate(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6" data-testid="api-key-rotate-dialog">
            <div className="flex items-center gap-3 mb-4">
              <RefreshCw className="h-6 w-6 text-amber-500" />
              <h3 className="text-lg font-semibold">Rotate API Key</h3>
            </div>
            <p className="text-muted-foreground mb-4">
              Rotating <strong>{keyToRotate.name}</strong> will generate a new
              secret key. You can optionally keep the old key valid for a grace
              period.
            </p>
            <div className="space-y-2 mb-4">
              <Label>Grace Period (hours)</Label>
              <Input
                type="number"
                min={0}
                max={168}
                value={rotateGracePeriod}
                onChange={(e) =>
                  setRotateGracePeriod(parseInt(e.target.value, 10) || 0)
                }
                placeholder="0 = immediate (no grace period)"
                data-testid="api-key-rotate-grace-period"
              />
              <p className="text-xs text-muted-foreground">
                Set to 0 for immediate rotation, or enter hours to keep the old
                key valid
              </p>
            </div>
            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setKeyToRotate(null)} data-testid="api-key-rotate-cancel">
                Cancel
              </Button>
              <Button onClick={handleRotateKey} disabled={rotatingKey} data-testid="api-key-rotate-confirm">
                {rotatingKey ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <RefreshCw className="h-4 w-4 mr-2" />
                )}
                Rotate Key
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
