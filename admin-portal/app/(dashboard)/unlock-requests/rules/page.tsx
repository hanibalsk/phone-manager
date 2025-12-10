"use client";

import Link from "next/link";
import { Button } from "@/components/ui/button";
import { ArrowLeft, Settings } from "lucide-react";

// Placeholder page - will be fully implemented in AP-8.5
export default function AutoApprovalRulesPage() {
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

      <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
        <Settings className="h-16 w-16 mb-4 opacity-50" />
        <h2 className="text-xl font-semibold mb-2">Auto-Approval Rules</h2>
        <p>Configure automatic approval rules for unlock requests.</p>
        <p className="text-sm mt-2">(Full implementation in Story AP-8.5)</p>
      </div>
    </div>
  );
}
