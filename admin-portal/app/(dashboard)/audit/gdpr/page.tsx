"use client";

import { useState, useEffect, useCallback } from "react";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Search,
  Download,
  RefreshCw,
  User,
  ArrowLeft,
  Trash2,
  FileText,
  Shield,
  AlertTriangle,
  Check,
  X,
  Clock,
  CheckCircle,
  XCircle,
} from "lucide-react";
import Link from "next/link";
import { useApi } from "@/hooks/use-api";
import { auditApi, usersApi } from "@/lib/api-client";
import type {
  AdminUser,
  GDPRDataExportRequest,
  GDPRDeletionRequest,
} from "@/types";

const DATA_TYPES = [
  { id: "profile", label: "Profile Information", description: "Name, email, preferences" },
  { id: "locations", label: "Location Data", description: "GPS history, geofence data" },
  { id: "devices", label: "Device Information", description: "Registered devices, settings" },
  { id: "activity", label: "Activity Logs", description: "Login history, actions taken" },
  { id: "trips", label: "Trip Data", description: "Travel history, routes" },
  { id: "organizations", label: "Organization Data", description: "Memberships, roles" },
];

const STATUS_BADGES = {
  pending: { label: "Pending", className: "bg-yellow-100 text-yellow-800", icon: Clock },
  processing: { label: "Processing", className: "bg-blue-100 text-blue-800", icon: RefreshCw },
  completed: { label: "Completed", className: "bg-green-100 text-green-800", icon: CheckCircle },
  failed: { label: "Failed", className: "bg-red-100 text-red-800", icon: XCircle },
};

function RequestStatusBadge({
  status,
}: {
  status: "pending" | "processing" | "completed" | "failed";
}) {
  const config = STATUS_BADGES[status];
  const Icon = config.icon;
  return (
    <Badge className={config.className}>
      <Icon className="h-3 w-3 mr-1" />
      {config.label}
    </Badge>
  );
}

export default function GDPRPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [userSearch, setUserSearch] = useState("");
  const [exportRequests, setExportRequests] = useState<GDPRDataExportRequest[]>([]);
  const [deletionRequests, setDeletionRequests] = useState<GDPRDeletionRequest[]>([]);

  // Data selection
  const [selectedDataTypes, setSelectedDataTypes] = useState<string[]>([]);

  // Dialogs
  const [exportDialogOpen, setExportDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [confirmDeleteStep, setConfirmDeleteStep] = useState(0);
  const [deleteConfirmText, setDeleteConfirmText] = useState("");

  const { execute: fetchUsers, loading: usersLoading } = useApi<{
    items: AdminUser[];
    total: number;
  }>();
  const { execute: fetchExports, loading: exportsLoading } =
    useApi<GDPRDataExportRequest[]>();
  const { execute: fetchDeletions, loading: deletionsLoading } =
    useApi<GDPRDeletionRequest[]>();
  const { execute: createExport, loading: creatingExport } =
    useApi<GDPRDataExportRequest>();
  const { execute: createDeletion, loading: creatingDeletion } =
    useApi<GDPRDeletionRequest>();

  const loadUsers = useCallback(async () => {
    const result = await fetchUsers(() => usersApi.list({ page: 1, limit: 50, search: userSearch }));
    if (result) {
      setUsers(result.items);
    }
  }, [fetchUsers, userSearch]);

  const loadRequests = useCallback(async () => {
    const [exports, deletions] = await Promise.all([
      fetchExports(() => auditApi.getDataExports()),
      fetchDeletions(() => auditApi.getDeletionRequests()),
    ]);
    if (exports) setExportRequests(exports);
    if (deletions) setDeletionRequests(deletions);
  }, [fetchExports, fetchDeletions]);

  useEffect(() => {
    loadUsers();
    loadRequests();
  }, [loadUsers, loadRequests]);

  const handleUserSearch = () => {
    loadUsers();
  };

  const handleSelectUser = (user: AdminUser) => {
    setSelectedUser(user);
    setSelectedDataTypes([]);
  };

  const toggleDataType = (id: string) => {
    setSelectedDataTypes((prev) =>
      prev.includes(id) ? prev.filter((t) => t !== id) : [...prev, id]
    );
  };

  const selectAllDataTypes = () => {
    setSelectedDataTypes(DATA_TYPES.map((t) => t.id));
  };

  const handleExportData = async () => {
    if (!selectedUser || selectedDataTypes.length === 0) return;
    await createExport(() =>
      auditApi.createDataExport(selectedUser.id, selectedDataTypes)
    );
    setExportDialogOpen(false);
    setSelectedDataTypes([]);
    loadRequests();
  };

  const handleDeleteData = async () => {
    if (!selectedUser || selectedDataTypes.length === 0) return;
    await createDeletion(() =>
      auditApi.createDeletionRequest(selectedUser.id, selectedDataTypes)
    );
    setDeleteDialogOpen(false);
    setConfirmDeleteStep(0);
    setDeleteConfirmText("");
    setSelectedDataTypes([]);
    loadRequests();
  };

  const openExportDialog = () => {
    if (selectedUser) {
      setSelectedDataTypes([]);
      setExportDialogOpen(true);
    }
  };

  const openDeleteDialog = () => {
    if (selectedUser) {
      setSelectedDataTypes([]);
      setConfirmDeleteStep(0);
      setDeleteConfirmText("");
      setDeleteDialogOpen(true);
    }
  };

  return (
    <div className="space-y-6" data-testid="gdpr-page">
      {/* Header */}
      <div className="flex items-center justify-between" data-testid="gdpr-header">
        <div>
          <div className="flex items-center gap-2 text-sm text-muted-foreground mb-2">
            <Link href="/" className="hover:text-foreground">
              Dashboard
            </Link>
            <span>/</span>
            <Link href="/audit" className="hover:text-foreground">
              Audit
            </Link>
            <span>/</span>
            <span>GDPR Compliance</span>
          </div>
          <h1 className="text-3xl font-bold">GDPR Compliance</h1>
          <p className="text-muted-foreground mt-1">
            Process data export and deletion requests
          </p>
        </div>
        <Link href="/audit">
          <Button variant="outline" data-testid="gdpr-back-button">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Back to Audit
          </Button>
        </Link>
      </div>

      {/* Info Banner */}
      <Card className="bg-blue-50 border-blue-200" data-testid="gdpr-info-banner">
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <Shield className="h-5 w-5 text-blue-600 mt-0.5" />
            <div>
              <p className="font-medium text-blue-900">GDPR Data Rights</p>
              <p className="text-sm text-blue-700 mt-1">
                Under GDPR, users have the right to access their data (Article 15) and
                request deletion (Article 17 - &quot;Right to be forgotten&quot;). All requests are
                logged for compliance auditing.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* User Selection */}
        <Card className="lg:col-span-1" data-testid="gdpr-user-selection-card">
          <CardHeader>
            <CardTitle className="text-lg">Select User</CardTitle>
            <CardDescription>
              Search for a user to process their data request
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex gap-2">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Search by name or email..."
                  value={userSearch}
                  onChange={(e) => setUserSearch(e.target.value)}
                  className="pl-10"
                  onKeyDown={(e) => e.key === "Enter" && handleUserSearch()}
                  data-testid="gdpr-user-search-input"
                />
              </div>
              <Button variant="outline" onClick={handleUserSearch} data-testid="gdpr-user-search-button">
                <Search className="h-4 w-4" />
              </Button>
            </div>

            <div className="space-y-2 max-h-[300px] overflow-y-auto" data-testid="gdpr-user-list">
              {usersLoading ? (
                <div className="text-center py-4" data-testid="gdpr-user-loading">
                  <RefreshCw className="h-5 w-5 animate-spin mx-auto text-muted-foreground" />
                </div>
              ) : users.length === 0 ? (
                <p className="text-sm text-muted-foreground text-center py-4" data-testid="gdpr-user-empty">
                  No users found
                </p>
              ) : (
                users.map((user) => (
                  <button
                    key={user.id}
                    onClick={() => handleSelectUser(user)}
                    className={`w-full p-3 text-left rounded-lg border transition-colors ${
                      selectedUser?.id === user.id
                        ? "border-primary bg-primary/5"
                        : "border-border hover:border-primary/50"
                    }`}
                    data-testid={`gdpr-user-item-${user.id}`}
                  >
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-muted flex items-center justify-center">
                        <User className="h-4 w-4 text-muted-foreground" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm truncate">{user.display_name}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {user.email}
                        </p>
                      </div>
                    </div>
                  </button>
                ))
              )}
            </div>

            {selectedUser && (
              <div className="pt-4 border-t space-y-2" data-testid="gdpr-selected-user-actions">
                <p className="text-sm font-medium">Selected: {selectedUser.display_name}</p>
                <div className="flex gap-2">
                  <Button
                    size="sm"
                    variant="outline"
                    className="flex-1"
                    onClick={openExportDialog}
                    data-testid="gdpr-export-button"
                  >
                    <Download className="h-4 w-4 mr-2" />
                    Export Data
                  </Button>
                  <Button
                    size="sm"
                    variant="destructive"
                    className="flex-1"
                    onClick={openDeleteDialog}
                    data-testid="gdpr-delete-button"
                  >
                    <Trash2 className="h-4 w-4 mr-2" />
                    Delete Data
                  </Button>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Request History */}
        <div className="lg:col-span-2 space-y-6" data-testid="gdpr-requests-section">
          {/* Export Requests */}
          <Card data-testid="gdpr-export-requests-card">
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg">Data Export Requests</CardTitle>
                  <CardDescription>Recent data export requests</CardDescription>
                </div>
                <Button variant="outline" size="sm" onClick={loadRequests} data-testid="gdpr-export-refresh">
                  <RefreshCw
                    className={`h-4 w-4 ${exportsLoading ? "animate-spin" : ""}`}
                  />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <Table data-testid="gdpr-export-requests-table">
                <TableHeader>
                  <TableRow>
                    <TableHead>User</TableHead>
                    <TableHead>Data Types</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Requested</TableHead>
                    <TableHead className="text-right">Action</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {exportRequests.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={5}
                        className="text-center text-muted-foreground py-8"
                      >
                        No export requests
                      </TableCell>
                    </TableRow>
                  ) : (
                    exportRequests.map((req) => (
                      <TableRow key={req.id}>
                        <TableCell>
                          <p className="font-medium text-sm">{req.user_email}</p>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1">
                            {req.data_types.slice(0, 2).map((type) => (
                              <Badge key={type} variant="secondary" className="text-xs">
                                {type}
                              </Badge>
                            ))}
                            {req.data_types.length > 2 && (
                              <Badge variant="secondary" className="text-xs">
                                +{req.data_types.length - 2}
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell>
                          <RequestStatusBadge status={req.status} />
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {new Date(req.created_at).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="text-right">
                          {req.status === "completed" && req.download_url && (
                            <Button size="sm" variant="ghost">
                              <Download className="h-4 w-4" />
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          {/* Deletion Requests */}
          <Card data-testid="gdpr-deletion-requests-card">
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="text-lg">Data Deletion Requests</CardTitle>
                  <CardDescription>Recent data deletion requests</CardDescription>
                </div>
                <Button variant="outline" size="sm" onClick={loadRequests} data-testid="gdpr-deletion-refresh">
                  <RefreshCw
                    className={`h-4 w-4 ${deletionsLoading ? "animate-spin" : ""}`}
                  />
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <Table data-testid="gdpr-deletion-requests-table">
                <TableHeader>
                  <TableRow>
                    <TableHead>User</TableHead>
                    <TableHead>Data Types</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Requested</TableHead>
                    <TableHead className="text-right">Report</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {deletionRequests.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={5}
                        className="text-center text-muted-foreground py-8"
                      >
                        No deletion requests
                      </TableCell>
                    </TableRow>
                  ) : (
                    deletionRequests.map((req) => (
                      <TableRow key={req.id}>
                        <TableCell>
                          <p className="font-medium text-sm">{req.user_email}</p>
                        </TableCell>
                        <TableCell>
                          <div className="flex flex-wrap gap-1">
                            {req.data_types_deleted.slice(0, 2).map((type) => (
                              <Badge key={type} variant="secondary" className="text-xs">
                                {type}
                              </Badge>
                            ))}
                            {req.data_types_deleted.length > 2 && (
                              <Badge variant="secondary" className="text-xs">
                                +{req.data_types_deleted.length - 2}
                              </Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell>
                          <RequestStatusBadge status={req.status} />
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {new Date(req.created_at).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="text-right">
                          {req.status === "completed" && req.verification_report && (
                            <Button size="sm" variant="ghost">
                              <FileText className="h-4 w-4" />
                            </Button>
                          )}
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Export Dialog */}
      <Dialog open={exportDialogOpen} onOpenChange={setExportDialogOpen}>
        <DialogContent className="sm:max-w-[500px]" data-testid="gdpr-export-dialog">
          <DialogHeader>
            <DialogTitle>Export User Data</DialogTitle>
            <DialogDescription>
              Select the data types to include in the export for {selectedUser?.display_name}
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="flex justify-between items-center">
              <Label>Data Types</Label>
              <Button variant="link" size="sm" onClick={selectAllDataTypes} data-testid="gdpr-export-select-all">
                Select All
              </Button>
            </div>
            <div className="space-y-3" data-testid="gdpr-export-data-types">
              {DATA_TYPES.map((type) => (
                <div
                  key={type.id}
                  className="flex items-start space-x-3 p-3 border rounded-lg"
                >
                  <Checkbox
                    id={type.id}
                    checked={selectedDataTypes.includes(type.id)}
                    onCheckedChange={() => toggleDataType(type.id)}
                  />
                  <div className="flex-1">
                    <label
                      htmlFor={type.id}
                      className="text-sm font-medium cursor-pointer"
                    >
                      {type.label}
                    </label>
                    <p className="text-xs text-muted-foreground">
                      {type.description}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setExportDialogOpen(false)} data-testid="gdpr-export-cancel">
              Cancel
            </Button>
            <Button
              onClick={handleExportData}
              disabled={selectedDataTypes.length === 0 || creatingExport}
              data-testid="gdpr-export-confirm"
            >
              {creatingExport && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
              Export Data
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Dialog */}
      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent className="sm:max-w-[500px]" data-testid="gdpr-delete-dialog">
          <DialogHeader>
            <DialogTitle className="text-red-600 flex items-center gap-2">
              <AlertTriangle className="h-5 w-5" />
              Delete User Data
            </DialogTitle>
            <DialogDescription>
              This action is irreversible. Please proceed with caution.
            </DialogDescription>
          </DialogHeader>

          {confirmDeleteStep === 0 && (
            <>
              <div className="py-4 space-y-4" data-testid="gdpr-delete-step-1">
                <div className="flex justify-between items-center">
                  <Label>Data Types to Delete</Label>
                  <Button variant="link" size="sm" onClick={selectAllDataTypes} data-testid="gdpr-delete-select-all">
                    Select All
                  </Button>
                </div>
                <div className="space-y-3" data-testid="gdpr-delete-data-types">
                  {DATA_TYPES.map((type) => (
                    <div
                      key={type.id}
                      className="flex items-start space-x-3 p-3 border rounded-lg"
                    >
                      <Checkbox
                        id={`del-${type.id}`}
                        checked={selectedDataTypes.includes(type.id)}
                        onCheckedChange={() => toggleDataType(type.id)}
                      />
                      <div className="flex-1">
                        <label
                          htmlFor={`del-${type.id}`}
                          className="text-sm font-medium cursor-pointer"
                        >
                          {type.label}
                        </label>
                        <p className="text-xs text-muted-foreground">
                          {type.description}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setDeleteDialogOpen(false)} data-testid="gdpr-delete-cancel">
                  Cancel
                </Button>
                <Button
                  variant="destructive"
                  onClick={() => setConfirmDeleteStep(1)}
                  disabled={selectedDataTypes.length === 0}
                  data-testid="gdpr-delete-continue"
                >
                  Continue
                </Button>
              </DialogFooter>
            </>
          )}

          {confirmDeleteStep === 1 && (
            <>
              <div className="py-4 space-y-4" data-testid="gdpr-delete-step-2">
                <div className="p-4 bg-red-50 border border-red-200 rounded-lg" data-testid="gdpr-delete-warning">
                  <p className="text-sm text-red-800 font-medium mb-2">
                    You are about to permanently delete:
                  </p>
                  <ul className="text-sm text-red-700 space-y-1">
                    {selectedDataTypes.map((type) => (
                      <li key={type} className="flex items-center gap-2">
                        <X className="h-3 w-3" />
                        {DATA_TYPES.find((t) => t.id === type)?.label}
                      </li>
                    ))}
                  </ul>
                </div>
                <div>
                  <Label htmlFor="confirm">
                    Type <strong>DELETE</strong> to confirm
                  </Label>
                  <Input
                    id="confirm"
                    value={deleteConfirmText}
                    onChange={(e) => setDeleteConfirmText(e.target.value)}
                    placeholder="Type DELETE"
                    className="mt-2"
                    data-testid="gdpr-delete-confirm-input"
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setConfirmDeleteStep(0)} data-testid="gdpr-delete-back">
                  Back
                </Button>
                <Button
                  variant="destructive"
                  onClick={handleDeleteData}
                  disabled={deleteConfirmText !== "DELETE" || creatingDeletion}
                  data-testid="gdpr-delete-confirm"
                >
                  {creatingDeletion && (
                    <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  )}
                  Permanently Delete
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
