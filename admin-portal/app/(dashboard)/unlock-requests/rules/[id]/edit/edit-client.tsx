"use client";

import { useEffect, useState } from "react";
import type { AutoApprovalRule } from "@/types";
import { AutoApprovalRuleForm } from "@/components/unlock-requests";
import { unlockRequestsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";

interface Props {
  ruleId: string;
}

export function EditAutoApprovalRuleClient({ ruleId }: Props) {
  const [rule, setRule] = useState<AutoApprovalRule | null>(null);

  const { loading, execute: fetchRule } = useApi<AutoApprovalRule>();

  useEffect(() => {
    if (ruleId) {
      fetchRule(async () => {
        const response = await unlockRequestsApi.getAutoApprovalRule(ruleId);
        if (response.data) {
          setRule(response.data);
        }
        return response;
      });
    }
  }, [ruleId, fetchRule]);

  if (loading) {
    return (
      <div className="container mx-auto py-6">
        <div className="flex justify-center py-8">
          <div className="text-muted-foreground">Loading rule...</div>
        </div>
      </div>
    );
  }

  if (!rule) {
    return (
      <div className="container mx-auto py-6">
        <div className="flex justify-center py-8">
          <div className="text-muted-foreground">Rule not found</div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6">
      <AutoApprovalRuleForm rule={rule} />
    </div>
  );
}
