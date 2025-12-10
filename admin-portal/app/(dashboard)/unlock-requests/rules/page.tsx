"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";
import { AutoApprovalRules, AutoApprovalLog } from "@/components/unlock-requests";

export default function AutoApprovalRulesPage() {
  const [activeTab, setActiveTab] = useState<"rules" | "log">("rules");

  return (
    <div className="container mx-auto py-6">
      <div className="mb-6">
        <Link href="/unlock-requests">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Back to Unlock Requests
          </Button>
        </Link>
      </div>

      {/* Tab Navigation */}
      <div className="flex gap-2 mb-6">
        <Button
          variant={activeTab === "rules" ? "default" : "outline"}
          onClick={() => setActiveTab("rules")}
        >
          Rules
        </Button>
        <Button
          variant={activeTab === "log" ? "default" : "outline"}
          onClick={() => setActiveTab("log")}
        >
          Audit Log
        </Button>
      </div>

      {activeTab === "rules" ? <AutoApprovalRules /> : <AutoApprovalLog />}
    </div>
  );
}
