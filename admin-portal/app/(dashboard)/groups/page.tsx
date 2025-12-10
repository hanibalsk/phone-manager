import { AdminGroupList } from "@/components/groups";

export default function GroupsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Groups Administration</h1>
        <p className="text-muted-foreground">
          View and manage all groups across organizations
        </p>
      </div>

      <AdminGroupList />
    </div>
  );
}
