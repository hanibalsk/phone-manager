"use client";

import { useState, useId } from "react";
import type { Organization, OrganizationType, UpdateOrganizationRequest } from "@/types";
import { organizationsApi } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { X, Building2, AlertCircle } from "lucide-react";
import { useFocusTrap } from "@/hooks/use-focus-trap";

interface OrganizationEditDialogProps {
  organization: Organization;
  onSuccess: () => void;
  onCancel: () => void;
}

const TYPE_OPTIONS: { value: OrganizationType; label: string }[] = [
  { value: "enterprise", label: "Enterprise" },
  { value: "smb", label: "SMB" },
  { value: "startup", label: "Startup" },
  { value: "personal", label: "Personal" },
];

interface FormErrors {
  name?: string;
  contact_email?: string;
  general?: string;
}

export function OrganizationEditDialog({ organization, onSuccess, onCancel }: OrganizationEditDialogProps) {
  const [name, setName] = useState(organization.name);
  const [type, setType] = useState<OrganizationType>(organization.type);
  const [contactEmail, setContactEmail] = useState(organization.contact_email);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<FormErrors>({});

  const dialogRef = useFocusTrap<HTMLDivElement>({ onEscape: onCancel });
  const titleId = useId();
  const descriptionId = useId();

  const validateForm = (): boolean => {
    const newErrors: FormErrors = {};

    if (!name.trim()) {
      newErrors.name = "Organization name is required";
    }

    if (!contactEmail.trim()) {
      newErrors.contact_email = "Contact email is required";
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(contactEmail)) {
      newErrors.contact_email = "Please enter a valid email address";
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!validateForm()) {
      return;
    }

    setSubmitting(true);
    setErrors({});

    try {
      const data: UpdateOrganizationRequest = {};

      if (name.trim() !== organization.name) {
        data.name = name.trim();
      }
      if (type !== organization.type) {
        data.type = type;
      }
      if (contactEmail.trim() !== organization.contact_email) {
        data.contact_email = contactEmail.trim();
      }

      // Only call API if there are changes
      if (Object.keys(data).length === 0) {
        onSuccess();
        return;
      }

      const result = await organizationsApi.update(organization.id, data);

      if (result.error) {
        setErrors({ general: result.error });
        return;
      }

      onSuccess();
    } catch (error) {
      setErrors({
        general: error instanceof Error ? error.message : "Failed to update organization"
      });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descriptionId}
    >
      <Card ref={dialogRef} className="w-full max-w-md mx-4">
        <form onSubmit={handleSubmit}>
          <CardHeader className="relative">
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="absolute right-4 top-4"
              onClick={onCancel}
              aria-label="Close dialog"
            >
              <X className="h-4 w-4" aria-hidden="true" />
            </Button>
            <CardTitle id={titleId} className="flex items-center gap-2">
              <Building2 className="h-5 w-5" aria-hidden="true" />
              Edit Organization
            </CardTitle>
            <CardDescription id={descriptionId}>
              Update organization details for {organization.name}
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            {errors.general && (
              <div className="flex items-center gap-2 p-3 text-sm text-destructive bg-destructive/10 rounded-lg">
                <AlertCircle className="h-4 w-4" />
                {errors.general}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="name">
                Name <span className="text-destructive">*</span>
              </Label>
              <Input
                id="name"
                placeholder="Acme Corporation"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (errors.name) setErrors({ ...errors, name: undefined });
                }}
                className={errors.name ? "border-destructive" : ""}
              />
              {errors.name && (
                <p className="text-sm text-destructive">{errors.name}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="slug">Slug</Label>
              <Input
                id="slug"
                value={organization.slug}
                disabled
                className="bg-muted"
              />
              <p className="text-xs text-muted-foreground">
                Slug cannot be changed after creation
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="type">Type</Label>
              <select
                id="type"
                className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                value={type}
                onChange={(e) => setType(e.target.value as OrganizationType)}
              >
                {TYPE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="contactEmail">
                Contact Email <span className="text-destructive">*</span>
              </Label>
              <Input
                id="contactEmail"
                type="email"
                placeholder="admin@acme.com"
                value={contactEmail}
                onChange={(e) => {
                  setContactEmail(e.target.value);
                  if (errors.contact_email) setErrors({ ...errors, contact_email: undefined });
                }}
                className={errors.contact_email ? "border-destructive" : ""}
              />
              {errors.contact_email && (
                <p className="text-sm text-destructive">{errors.contact_email}</p>
              )}
            </div>
          </CardContent>

          <CardFooter className="flex justify-end gap-2">
            <Button
              type="button"
              variant="outline"
              onClick={onCancel}
              disabled={submitting}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={submitting}>
              {submitting ? "Saving..." : "Save Changes"}
            </Button>
          </CardFooter>
        </form>
      </Card>
    </div>
  );
}
