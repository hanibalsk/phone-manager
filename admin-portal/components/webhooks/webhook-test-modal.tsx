"use client";

import { useState } from "react";
import type { Webhook, WebhookEventType, WebhookTestResult } from "@/types";
import { WebhookEventBadge } from "./webhook-event-badge";
import { webhooksApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import { Button } from "@/components/ui/button";
import {
  RefreshCw,
  PlayCircle,
  CheckCircle2,
  XCircle,
  Clock,
  Wifi,
  WifiOff,
  AlertCircle,
  X,
} from "lucide-react";

interface WebhookTestModalProps {
  webhook: Webhook;
  onClose: () => void;
}

export function WebhookTestModal({ webhook, onClose }: WebhookTestModalProps) {
  const [selectedEventType, setSelectedEventType] = useState<WebhookEventType>(
    webhook.event_types[0] || "location_update"
  );
  const [testResult, setTestResult] = useState<WebhookTestResult | null>(null);

  const { loading, error, execute } = useApi<WebhookTestResult>();

  const handleTest = async () => {
    setTestResult(null);
    const result = await execute(() => webhooksApi.test(webhook.id, selectedEventType));
    if (result) {
      setTestResult(result);
    }
  };

  const formatResponseTime = (ms: number | null) => {
    if (ms === null) return "N/A";
    if (ms < 1000) return `${ms}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  const getAccessibilityIcon = (accessibility: WebhookTestResult["accessibility"]) => {
    switch (accessibility) {
      case "reachable":
        return <Wifi className="h-4 w-4 text-green-600" />;
      case "unreachable":
        return <WifiOff className="h-4 w-4 text-red-600" />;
      case "timeout":
        return <Clock className="h-4 w-4 text-yellow-600" />;
    }
  };

  const getAccessibilityLabel = (accessibility: WebhookTestResult["accessibility"]) => {
    switch (accessibility) {
      case "reachable":
        return "Endpoint reachable";
      case "unreachable":
        return "Endpoint unreachable";
      case "timeout":
        return "Connection timeout";
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
    >
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className="relative bg-background rounded-lg shadow-lg w-full max-w-lg mx-4 max-h-[80vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-6 border-b">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Test Webhook</h2>
            <Button variant="ghost" size="sm" onClick={onClose}>
              <X className="h-4 w-4" />
            </Button>
          </div>
          <p className="text-sm text-muted-foreground mt-1">{webhook.name}</p>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto flex-1">
          {/* Event Type Selection */}
          <div className="mb-6">
            <label className="block text-sm font-medium mb-2">
              Select Event Type to Test
            </label>
            <div className="flex flex-wrap gap-2">
              {webhook.event_types.map((type) => (
                <button
                  key={type}
                  onClick={() => setSelectedEventType(type)}
                  className={`transition-all ${
                    selectedEventType === type
                      ? "ring-2 ring-primary ring-offset-2 rounded-md"
                      : ""
                  }`}
                >
                  <WebhookEventBadge eventType={type} />
                </button>
              ))}
            </div>
          </div>

          {/* URL Display */}
          <div className="mb-6">
            <label className="block text-sm font-medium mb-2">Endpoint URL</label>
            <code className="block text-xs bg-muted p-3 rounded-md break-all">
              {webhook.url}
            </code>
          </div>

          {/* Test Button */}
          <div className="mb-6">
            <Button onClick={handleTest} disabled={loading} className="w-full">
              {loading ? (
                <>
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  Sending Test...
                </>
              ) : (
                <>
                  <PlayCircle className="h-4 w-4 mr-2" />
                  Send Test Event
                </>
              )}
            </Button>
          </div>

          {/* Error */}
          {error && (
            <div className="mb-6 p-3 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-md">
              <div className="flex items-center gap-2 text-red-800 dark:text-red-400">
                <AlertCircle className="h-4 w-4" />
                <span className="text-sm font-medium">Test Failed</span>
              </div>
              <p className="text-sm text-red-700 dark:text-red-300 mt-1">{error}</p>
            </div>
          )}

          {/* Test Result */}
          {testResult && (
            <div className="space-y-4">
              {/* Overall Result */}
              <div
                className={`p-4 rounded-md ${
                  testResult.success
                    ? "bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800"
                    : "bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800"
                }`}
              >
                <div className="flex items-center gap-2">
                  {testResult.success ? (
                    <CheckCircle2 className="h-5 w-5 text-green-600" />
                  ) : (
                    <XCircle className="h-5 w-5 text-red-600" />
                  )}
                  <span className="font-medium">
                    {testResult.success ? "Test Successful" : "Test Failed"}
                  </span>
                </div>
              </div>

              {/* Details Grid */}
              <div className="grid grid-cols-2 gap-4">
                {/* Status Code */}
                <div className="p-3 bg-muted rounded-md">
                  <div className="text-xs text-muted-foreground mb-1">Status Code</div>
                  <div className="font-medium">
                    {testResult.status_code !== null ? (
                      <span
                        className={
                          testResult.status_code >= 200 && testResult.status_code < 300
                            ? "text-green-600"
                            : "text-red-600"
                        }
                      >
                        HTTP {testResult.status_code}
                      </span>
                    ) : (
                      "N/A"
                    )}
                  </div>
                </div>

                {/* Response Time */}
                <div className="p-3 bg-muted rounded-md">
                  <div className="text-xs text-muted-foreground mb-1">Response Time</div>
                  <div className="font-medium">
                    {formatResponseTime(testResult.response_time_ms)}
                  </div>
                </div>

                {/* Accessibility */}
                <div className="p-3 bg-muted rounded-md col-span-2">
                  <div className="text-xs text-muted-foreground mb-1">
                    Endpoint Accessibility
                  </div>
                  <div className="flex items-center gap-2 font-medium">
                    {getAccessibilityIcon(testResult.accessibility)}
                    {getAccessibilityLabel(testResult.accessibility)}
                  </div>
                </div>
              </div>

              {/* Error Message */}
              {testResult.error_message && (
                <div>
                  <div className="text-xs text-muted-foreground mb-1">Error Message</div>
                  <div className="text-sm bg-red-50 dark:bg-red-900/20 text-red-800 dark:text-red-400 p-3 rounded-md">
                    {testResult.error_message}
                  </div>
                </div>
              )}

              {/* Response Body */}
              {testResult.response_body && (
                <div>
                  <div className="text-xs text-muted-foreground mb-1">Response Body</div>
                  <pre className="text-xs bg-muted p-3 rounded-md overflow-auto max-h-[150px]">
                    {(() => {
                      try {
                        return JSON.stringify(JSON.parse(testResult.response_body), null, 2);
                      } catch {
                        return testResult.response_body;
                      }
                    })()}
                  </pre>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-6 border-t">
          <div className="flex justify-end">
            <Button variant="outline" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
