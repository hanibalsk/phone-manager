import { AdminWebhookList } from "@/components/webhooks";

export default function WebhooksPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Webhooks</h1>
        <p className="text-muted-foreground">
          Manage webhook integrations for event notifications
        </p>
      </div>
      <AdminWebhookList />
    </div>
  );
}
