import { EditAutoApprovalRuleClient } from "./edit-client";

// Generate a placeholder path for static export
// Actual routing happens client-side
export function generateStaticParams() {
  return [{ id: "_" }];
}

interface Props {
  params: Promise<{ id: string }>;
}

export default async function EditAutoApprovalRulePage({ params }: Props) {
  const { id } = await params;
  return <EditAutoApprovalRuleClient ruleId={id} />;
}
