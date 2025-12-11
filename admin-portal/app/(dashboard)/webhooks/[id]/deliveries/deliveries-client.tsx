"use client";

import { useEffect, useState } from "react";
import { WebhookDeliveryLog } from "@/components/webhooks";
import { webhooksApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";
import type { Webhook } from "@/types";
import { RefreshCw } from "lucide-react";

interface Props {
  webhookId: string;
}

export function WebhookDeliveriesClient({ webhookId }: Props) {
  const [webhookName, setWebhookName] = useState<string>("");

  const { loading, error, execute } = useApi<Webhook>();

  useEffect(() => {
    if (webhookId) {
      execute(() => webhooksApi.get(webhookId)).then((result) => {
        if (result) {
          setWebhookName(result.name);
        }
      });
    }
  }, [webhookId, execute]);

  if (loading && !webhookName) {
    return (
      <div className="flex items-center justify-center py-12">
        <RefreshCw className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <p className="text-destructive mb-4">Failed to load webhook: {error}</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Webhook Deliveries</h1>
        <p className="text-muted-foreground">
          View delivery history and resend failed deliveries
        </p>
      </div>
      <WebhookDeliveryLog webhookId={webhookId} webhookName={webhookName} />
    </div>
  );
}
