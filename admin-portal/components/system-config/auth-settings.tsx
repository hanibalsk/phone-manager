"use client";

import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useApi } from "@/hooks/use-api";
import { authConfigApi } from "@/lib/api-client";
import type {
  AuthConfig,
  RegistrationMode,
  OAuthProviderConfig,
  UpdateAuthConfigRequest,
  UpdateOAuthProviderRequest,
} from "@/types";
import {
  Save,
  Loader2,
  Shield,
  Users,
  Clock,
  Lock,
  AlertCircle,
  Check,
  X,
  Eye,
  EyeOff,
} from "lucide-react";

const registrationModes: { value: RegistrationMode; label: string; description: string }[] = [
  {
    value: "open",
    label: "Open Registration",
    description: "Anyone can create an account",
  },
  {
    value: "invite_only",
    label: "Invite Only",
    description: "Users must be invited to register",
  },
  {
    value: "oauth_only",
    label: "OAuth Only",
    description: "Users can only sign in via OAuth providers",
  },
  {
    value: "disabled",
    label: "Disabled",
    description: "New registrations are not allowed",
  },
];

// Quick presets for common settings
const sessionTimeoutPresets = [15, 30, 60, 120, 480, 1440]; // minutes
const lockoutDurationPresets = [5, 15, 30, 60]; // minutes

export function AuthSettings() {
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Auth config state
  const [config, setConfig] = useState<AuthConfig | null>(null);
  const [formData, setFormData] = useState<UpdateAuthConfigRequest>({});
  const [hasChanges, setHasChanges] = useState(false);

  // OAuth provider editing
  const [editingProvider, setEditingProvider] = useState<"google" | "apple" | null>(null);
  const [oauthFormData, setOAuthFormData] = useState<UpdateOAuthProviderRequest>({});
  const [showClientSecret, setShowClientSecret] = useState(false);

  // Confirmation modal for registration mode changes
  const [pendingModeChange, setPendingModeChange] = useState<RegistrationMode | null>(null);

  // API hooks
  const { loading: loadingConfig, execute: fetchConfig } = useApi<AuthConfig>();
  const { loading: savingConfig, execute: saveConfig } = useApi<AuthConfig>();
  const { loading: savingProvider, execute: saveProvider } = useApi<OAuthProviderConfig>();

  // Load initial config
  useEffect(() => {
    loadConfig();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadConfig = async () => {
    const result = await fetchConfig(() => authConfigApi.get());
    if (result) {
      setConfig(result);
      setFormData({
        registration_mode: result.registration_mode,
        session_timeout_minutes: result.session_timeout_minutes,
        max_login_attempts: result.max_login_attempts,
        lockout_duration_minutes: result.lockout_duration_minutes,
        require_mfa: result.require_mfa,
        password_min_length: result.password_min_length,
        password_require_special: result.password_require_special,
      });
      setHasChanges(false);
    }
  };

  const handleFormChange = (field: keyof UpdateAuthConfigRequest, value: unknown) => {
    // For registration mode, require confirmation
    if (field === "registration_mode" && value !== config?.registration_mode) {
      setPendingModeChange(value as RegistrationMode);
      return;
    }

    setFormData((prev) => ({ ...prev, [field]: value }));
    setHasChanges(true);
  };

  const confirmModeChange = () => {
    if (pendingModeChange) {
      setFormData((prev) => ({ ...prev, registration_mode: pendingModeChange }));
      setHasChanges(true);
      setPendingModeChange(null);
    }
  };

  const handleSaveConfig = async () => {
    const result = await saveConfig(() => authConfigApi.update(formData));
    if (result) {
      setConfig(result);
      setHasChanges(false);
      setNotification({ type: "success", message: "Authentication settings saved" });
      setTimeout(() => setNotification(null), 3000);
    } else {
      setNotification({ type: "error", message: "Failed to save settings" });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  const startEditingProvider = (provider: OAuthProviderConfig) => {
    setEditingProvider(provider.provider);
    setOAuthFormData({
      enabled: provider.enabled,
      client_id: provider.client_id,
      client_secret: "",
      allowed_domains: provider.allowed_domains,
    });
    setShowClientSecret(false);
  };

  const handleSaveProvider = async () => {
    if (!editingProvider) return;

    const result = await saveProvider(() =>
      authConfigApi.updateOAuthProvider(editingProvider, oauthFormData)
    );

    if (result) {
      // Update local config
      setConfig((prev) => {
        if (!prev) return prev;
        return {
          ...prev,
          oauth_providers: prev.oauth_providers.map((p) =>
            p.provider === editingProvider ? result : p
          ),
        };
      });
      setEditingProvider(null);
      setNotification({ type: "success", message: `${editingProvider} OAuth settings saved` });
      setTimeout(() => setNotification(null), 3000);
    } else {
      setNotification({ type: "error", message: "Failed to save OAuth provider settings" });
      setTimeout(() => setNotification(null), 3000);
    }
  };

  if (loadingConfig && !config) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-8" data-testid="auth-settings-container">
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

      {/* Registration Mode */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <Users className="h-5 w-5 text-muted-foreground" />
          <h3 className="text-lg font-semibold">Registration Mode</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {registrationModes.map((mode) => (
            <div
              key={mode.value}
              onClick={() => handleFormChange("registration_mode", mode.value)}
              className={`p-4 rounded-lg border cursor-pointer transition-colors ${
                formData.registration_mode === mode.value
                  ? "border-primary bg-primary/5"
                  : "border-border hover:border-primary/50"
              }`}
              data-testid={`registration-mode-${mode.value}`}
            >
              <div className="flex items-center justify-between">
                <span className="font-medium">{mode.label}</span>
                {formData.registration_mode === mode.value && (
                  <Check className="h-4 w-4 text-primary" />
                )}
              </div>
              <p className="text-sm text-muted-foreground mt-1">{mode.description}</p>
            </div>
          ))}
        </div>
      </div>

      {/* OAuth Providers */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <Shield className="h-5 w-5 text-muted-foreground" />
          <h3 className="text-lg font-semibold">OAuth Providers</h3>
        </div>

        <div className="space-y-3">
          {config?.oauth_providers.map((provider) => (
            <div
              key={provider.provider}
              className="p-4 rounded-lg border flex items-center justify-between"
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-10 h-10 rounded-full flex items-center justify-center ${
                    provider.provider === "google"
                      ? "bg-red-100"
                      : "bg-gray-100"
                  }`}
                >
                  {provider.provider === "google" ? "G" : ""}
                </div>
                <div>
                  <div className="font-medium capitalize">{provider.provider}</div>
                  <div className="text-sm text-muted-foreground">
                    {provider.enabled ? (
                      <span className="text-green-600">Enabled</span>
                    ) : (
                      <span className="text-muted-foreground">Disabled</span>
                    )}
                    {provider.client_secret_set && (
                      <span className="ml-2">• Secret configured</span>
                    )}
                  </div>
                </div>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => startEditingProvider(provider)}
                data-testid={`oauth-configure-${provider.provider}`}
              >
                Configure
              </Button>
            </div>
          ))}
        </div>
      </div>

      {/* Session Settings */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <Clock className="h-5 w-5 text-muted-foreground" />
          <h3 className="text-lg font-semibold">Session Settings</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2">
            <Label htmlFor="session_timeout">Session Timeout (minutes)</Label>
            <Input
              id="session_timeout"
              type="number"
              min={5}
              max={10080}
              value={formData.session_timeout_minutes || ""}
              onChange={(e) =>
                handleFormChange("session_timeout_minutes", parseInt(e.target.value, 10))
              }
              data-testid="session-timeout-input"
            />
            <div className="flex flex-wrap gap-1">
              {sessionTimeoutPresets.map((preset) => (
                <Button
                  key={preset}
                  type="button"
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs"
                  onClick={() => handleFormChange("session_timeout_minutes", preset)}
                >
                  {preset >= 60 ? `${preset / 60}h` : `${preset}m`}
                </Button>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="max_attempts">Max Login Attempts</Label>
            <Input
              id="max_attempts"
              type="number"
              min={3}
              max={20}
              value={formData.max_login_attempts || ""}
              onChange={(e) =>
                handleFormChange("max_login_attempts", parseInt(e.target.value, 10))
              }
              data-testid="max-login-attempts-input"
            />
            <p className="text-xs text-muted-foreground">
              Account locked after this many failed attempts
            </p>
          </div>
        </div>
      </div>

      {/* Security Settings */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <Lock className="h-5 w-5 text-muted-foreground" />
          <h3 className="text-lg font-semibold">Security Settings</h3>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-2">
            <Label htmlFor="lockout_duration">Lockout Duration (minutes)</Label>
            <Input
              id="lockout_duration"
              type="number"
              min={1}
              max={1440}
              value={formData.lockout_duration_minutes || ""}
              onChange={(e) =>
                handleFormChange("lockout_duration_minutes", parseInt(e.target.value, 10))
              }
              data-testid="lockout-duration-input"
            />
            <div className="flex flex-wrap gap-1">
              {lockoutDurationPresets.map((preset) => (
                <Button
                  key={preset}
                  type="button"
                  variant="outline"
                  size="sm"
                  className="h-7 text-xs"
                  onClick={() => handleFormChange("lockout_duration_minutes", preset)}
                >
                  {preset}m
                </Button>
              ))}
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="password_min_length">Min Password Length</Label>
            <Input
              id="password_min_length"
              type="number"
              min={6}
              max={32}
              value={formData.password_min_length || ""}
              onChange={(e) =>
                handleFormChange("password_min_length", parseInt(e.target.value, 10))
              }
              data-testid="password-min-length-input"
            />
          </div>
        </div>

        {/* Toggle options */}
        <div className="space-y-3">
          <div
            onClick={() => handleFormChange("require_mfa", !formData.require_mfa)}
            className={`p-4 rounded-lg border cursor-pointer transition-colors flex items-center justify-between ${
              formData.require_mfa
                ? "border-primary bg-primary/5"
                : "border-border hover:border-primary/50"
            }`}
            data-testid="require-mfa-toggle"
          >
            <div>
              <div className="font-medium">Require MFA</div>
              <p className="text-sm text-muted-foreground">
                Users must enable two-factor authentication
              </p>
            </div>
            <div
              className={`w-10 h-6 rounded-full transition-colors ${
                formData.require_mfa ? "bg-primary" : "bg-muted"
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white shadow transition-transform mt-0.5 ${
                  formData.require_mfa ? "translate-x-4.5 ml-0.5" : "translate-x-0.5"
                }`}
              />
            </div>
          </div>

          <div
            onClick={() =>
              handleFormChange("password_require_special", !formData.password_require_special)
            }
            className={`p-4 rounded-lg border cursor-pointer transition-colors flex items-center justify-between ${
              formData.password_require_special
                ? "border-primary bg-primary/5"
                : "border-border hover:border-primary/50"
            }`}
            data-testid="password-require-special-toggle"
          >
            <div>
              <div className="font-medium">Require Special Characters</div>
              <p className="text-sm text-muted-foreground">
                Passwords must include special characters
              </p>
            </div>
            <div
              className={`w-10 h-6 rounded-full transition-colors ${
                formData.password_require_special ? "bg-primary" : "bg-muted"
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white shadow transition-transform mt-0.5 ${
                  formData.password_require_special ? "translate-x-4.5 ml-0.5" : "translate-x-0.5"
                }`}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Save Button */}
      <div className="flex justify-end pt-4 border-t">
        <Button onClick={handleSaveConfig} disabled={!hasChanges || savingConfig} data-testid="auth-settings-save-button">
          {savingConfig ? (
            <Loader2 className="h-4 w-4 animate-spin mr-2" />
          ) : (
            <Save className="h-4 w-4 mr-2" />
          )}
          Save Changes
        </Button>
      </div>

      {/* Registration Mode Confirmation Modal */}
      {pendingModeChange && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          data-testid="registration-mode-confirm-dialog"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setPendingModeChange(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
            <div className="flex items-center gap-3 mb-4">
              <AlertCircle className="h-6 w-6 text-amber-500" />
              <h3 className="text-lg font-semibold">Confirm Registration Mode Change</h3>
            </div>
            <p className="text-muted-foreground mb-4">
              Are you sure you want to change the registration mode to{" "}
              <strong>
                {registrationModes.find((m) => m.value === pendingModeChange)?.label}
              </strong>
              ? This will affect how users can sign up for the platform.
            </p>
            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setPendingModeChange(null)} data-testid="registration-mode-cancel">
                Cancel
              </Button>
              <Button onClick={confirmModeChange} data-testid="registration-mode-confirm">Confirm Change</Button>
            </div>
          </div>
        </div>
      )}

      {/* OAuth Provider Edit Modal */}
      {editingProvider && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center"
          role="dialog"
          aria-modal="true"
          data-testid="oauth-provider-dialog"
        >
          <div
            className="absolute inset-0 bg-black/50"
            onClick={() => setEditingProvider(null)}
          />
          <div className="relative bg-background rounded-lg shadow-lg w-full max-w-lg mx-4 p-6">
            <h3 className="text-lg font-semibold mb-4 capitalize">
              Configure {editingProvider} OAuth
            </h3>

            <div className="space-y-4">
              {/* Enable/Disable */}
              <div
                onClick={() =>
                  setOAuthFormData((prev) => ({ ...prev, enabled: !prev.enabled }))
                }
                className={`p-4 rounded-lg border cursor-pointer transition-colors flex items-center justify-between ${
                  oauthFormData.enabled
                    ? "border-primary bg-primary/5"
                    : "border-border"
                }`}
              >
                <div className="font-medium">
                  {oauthFormData.enabled ? "Enabled" : "Disabled"}
                </div>
                <div
                  className={`w-10 h-6 rounded-full transition-colors ${
                    oauthFormData.enabled ? "bg-primary" : "bg-muted"
                  }`}
                >
                  <div
                    className={`w-5 h-5 rounded-full bg-white shadow transition-transform mt-0.5 ${
                      oauthFormData.enabled ? "translate-x-4.5 ml-0.5" : "translate-x-0.5"
                    }`}
                  />
                </div>
              </div>

              {/* Client ID */}
              <div className="space-y-2">
                <Label htmlFor="client_id">Client ID</Label>
                <Input
                  id="client_id"
                  type="text"
                  value={oauthFormData.client_id || ""}
                  onChange={(e) =>
                    setOAuthFormData((prev) => ({ ...prev, client_id: e.target.value }))
                  }
                  placeholder="Enter client ID"
                />
              </div>

              {/* Client Secret */}
              <div className="space-y-2">
                <Label htmlFor="client_secret">Client Secret</Label>
                <div className="relative">
                  <Input
                    id="client_secret"
                    type={showClientSecret ? "text" : "password"}
                    value={oauthFormData.client_secret || ""}
                    onChange={(e) =>
                      setOAuthFormData((prev) => ({
                        ...prev,
                        client_secret: e.target.value,
                      }))
                    }
                    placeholder="Leave blank to keep existing"
                    className="pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowClientSecret(!showClientSecret)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                  >
                    {showClientSecret ? (
                      <EyeOff className="h-4 w-4" />
                    ) : (
                      <Eye className="h-4 w-4" />
                    )}
                  </button>
                </div>
                <p className="text-xs text-muted-foreground">
                  Leave blank to keep the existing secret
                </p>
              </div>

              {/* Allowed Domains */}
              <div className="space-y-2">
                <Label htmlFor="allowed_domains">Allowed Domains (optional)</Label>
                <Input
                  id="allowed_domains"
                  type="text"
                  value={oauthFormData.allowed_domains?.join(", ") || ""}
                  onChange={(e) =>
                    setOAuthFormData((prev) => ({
                      ...prev,
                      allowed_domains: e.target.value
                        ? e.target.value.split(",").map((d) => d.trim())
                        : undefined,
                    }))
                  }
                  placeholder="example.com, company.org"
                />
                <p className="text-xs text-muted-foreground">
                  Comma-separated list of allowed email domains
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <Button variant="outline" onClick={() => setEditingProvider(null)} data-testid="oauth-provider-cancel">
                Cancel
              </Button>
              <Button onClick={handleSaveProvider} disabled={savingProvider} data-testid="oauth-provider-save">
                {savingProvider ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <Save className="h-4 w-4 mr-2" />
                )}
                Save
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
