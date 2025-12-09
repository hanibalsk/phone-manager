"use client";

import { Badge } from "@/components/ui/badge";
import type { OrganizationType } from "@/types";

interface OrganizationTypeBadgeProps {
  type: OrganizationType;
}

const typeConfig: Record<OrganizationType, { label: string; variant: "default" | "secondary" | "outline" }> = {
  enterprise: { label: "Enterprise", variant: "default" },
  smb: { label: "SMB", variant: "secondary" },
  startup: { label: "Startup", variant: "secondary" },
  personal: { label: "Personal", variant: "outline" },
};

export function OrganizationTypeBadge({ type }: OrganizationTypeBadgeProps) {
  const config = typeConfig[type];

  return (
    <Badge variant={config.variant}>
      {config.label}
    </Badge>
  );
}
