"use client";

import { useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { AlertTriangle, RefreshCw, type LucideIcon } from "lucide-react";

export interface ConfirmationDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description: string;
  icon?: LucideIcon;
  iconClassName?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: "default" | "destructive";
  loading?: boolean;
  /** If provided, user must type this text to confirm */
  confirmText?: string;
  /** Label shown above the confirmation input */
  confirmTextLabel?: ReactNode;
  /** Optional input field (e.g., for reason) */
  inputField?: {
    label: string;
    placeholder: string;
    value: string;
    onChange: (value: string) => void;
  };
  children?: ReactNode;
}

export function ConfirmationDialog({
  open,
  onClose,
  onConfirm,
  title,
  description,
  icon: Icon = AlertTriangle,
  iconClassName = "text-yellow-500",
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  variant = "default",
  loading = false,
  confirmText,
  confirmTextLabel,
  inputField,
  children,
}: ConfirmationDialogProps) {
  const [inputValue, setInputValue] = useState("");

  if (!open) return null;

  const canConfirm = confirmText ? inputValue === confirmText : true;

  const handleClose = () => {
    setInputValue("");
    onClose();
  };

  const handleConfirm = () => {
    if (!canConfirm || loading) return;
    onConfirm();
  };

  const borderClass = variant === "destructive" ? "border-destructive/50" : "border";
  const bgClass = variant === "destructive" ? "bg-destructive/5" : "";
  const titleClass = variant === "destructive" ? "text-destructive" : "";

  return (
    <div className={`p-4 ${borderClass} rounded-lg space-y-4 ${bgClass}`} data-testid="confirmation-dialog">
      <div className="flex items-start gap-3">
        <Icon className={`h-5 w-5 mt-0.5 ${iconClassName}`} />
        <div>
          <h4 className={`font-medium ${titleClass}`}>{title}</h4>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
      </div>

      {inputField && (
        <div>
          <label className="text-sm font-medium">{inputField.label}</label>
          <Input
            placeholder={inputField.placeholder}
            value={inputField.value}
            onChange={(e) => inputField.onChange(e.target.value)}
            className="mt-1"
            data-testid="confirmation-input-field"
          />
        </div>
      )}

      {confirmText && (
        <div>
          <label className="text-sm font-medium">
            {confirmTextLabel || (
              <>Type <span className="font-mono">{confirmText}</span> to confirm</>
            )}
          </label>
          <Input
            placeholder={`Enter "${confirmText}"...`}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            className="mt-1"
            data-testid="confirmation-text-input"
          />
        </div>
      )}

      {children}

      <div className="flex gap-2">
        <Button
          variant="outline"
          onClick={handleClose}
          disabled={loading}
          data-testid="confirmation-cancel-btn"
        >
          {cancelLabel}
        </Button>
        <Button
          variant={variant}
          onClick={handleConfirm}
          disabled={loading || !canConfirm}
          data-testid="confirmation-confirm-btn"
        >
          {loading && <RefreshCw className="h-4 w-4 mr-2 animate-spin" />}
          {confirmLabel}
        </Button>
      </div>
    </div>
  );
}
