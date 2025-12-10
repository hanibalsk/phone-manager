import { AdminTripList } from "@/components/trips";

export default function TripsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Trips</h1>
        <p className="text-muted-foreground">
          View and analyze trip data across all devices
        </p>
      </div>
      <AdminTripList />
    </div>
  );
}
