"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import type { LimitTemplate } from "@/types";
import { LimitTemplateForm } from "@/components/app-limits";
import { appLimitsApi } from "@/lib/api-client";
import { useApi } from "@/hooks/use-api";

export default function EditLimitTemplatePage() {
  const params = useParams();
  const templateId = params.id as string;
  const [template, setTemplate] = useState<LimitTemplate | null>(null);

  const { loading, execute: fetchTemplate } = useApi<LimitTemplate>();

  useEffect(() => {
    if (templateId) {
      fetchTemplate(async () => {
        const response = await appLimitsApi.getTemplate(templateId);
        if (response.data) {
          setTemplate(response.data);
        }
        return response;
      });
    }
  }, [templateId, fetchTemplate]);

  if (loading) {
    return (
      <div className="container mx-auto py-6">
        <div className="flex justify-center py-8">
          <div className="text-muted-foreground">Loading template...</div>
        </div>
      </div>
    );
  }

  if (!template) {
    return (
      <div className="container mx-auto py-6">
        <div className="flex justify-center py-8">
          <div className="text-muted-foreground">Template not found</div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6">
      <LimitTemplateForm template={template} />
    </div>
  );
}
