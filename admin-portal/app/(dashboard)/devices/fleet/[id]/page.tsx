import { DeviceDetailsClient } from "./device-client";

// Generate a placeholder path for static export
// Actual routing happens client-side
export function generateStaticParams() {
  return [{ id: "_" }];
}

interface Props {
  params: Promise<{ id: string }>;
}

export default async function DeviceDetailsPage({ params }: Props) {
  const { id } = await params;
  return <DeviceDetailsClient deviceId={id} />;
}
