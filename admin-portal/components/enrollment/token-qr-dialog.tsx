"use client";

import { useState } from "react";
import type { EnrollmentToken } from "@/types";
import { Button } from "@/components/ui/button";
import { X, Copy, Download, Check, QrCode } from "lucide-react";

interface TokenQrDialogProps {
  token: EnrollmentToken;
  onClose: () => void;
}

export function TokenQrDialog({ token, onClose }: TokenQrDialogProps) {
  const [copied, setCopied] = useState(false);

  // Generate enrollment URL
  const enrollmentUrl = `phonemanager://enroll?token=${token.code}`;

  // Generate QR code URL using QR code API
  const qrCodeUrl = `https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(enrollmentUrl)}`;

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(enrollmentUrl);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error("Failed to copy:", err);
    }
  };

  const handleDownloadQr = () => {
    const link = document.createElement("a");
    link.href = qrCodeUrl;
    link.download = `enrollment-token-${token.name.replace(/\s+/g, "-").toLowerCase()}.png`;
    link.click();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/50"
        onClick={onClose}
      />

      {/* Dialog */}
      <div className="relative bg-background rounded-lg shadow-lg w-full max-w-md mx-4 p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold flex items-center gap-2">
            <QrCode className="h-5 w-5" />
            Enrollment QR Code
          </h2>
          <Button variant="ghost" size="sm" onClick={onClose}>
            <X className="h-4 w-4" />
          </Button>
        </div>

        <div className="space-y-4">
          {/* Token Info */}
          <div className="text-center">
            <p className="font-medium">{token.name}</p>
            <p className="text-sm text-muted-foreground">
              {token.max_uses ? `${token.max_uses - token.uses_count} uses remaining` : "Unlimited uses"}
            </p>
          </div>

          {/* QR Code */}
          <div className="flex justify-center p-4 bg-white rounded-lg">
            <img
              src={qrCodeUrl}
              alt="Enrollment QR Code"
              className="w-48 h-48"
            />
          </div>

          {/* Token Code */}
          <div className="p-3 bg-muted rounded-lg">
            <label className="text-xs font-medium text-muted-foreground">Token Code</label>
            <p className="font-mono text-sm mt-1">{token.code}</p>
          </div>

          {/* Enrollment URL */}
          <div className="p-3 bg-muted rounded-lg">
            <label className="text-xs font-medium text-muted-foreground">Enrollment Link</label>
            <p className="font-mono text-xs mt-1 break-all">{enrollmentUrl}</p>
          </div>

          {/* Actions */}
          <div className="flex gap-2">
            <Button
              variant="outline"
              className="flex-1"
              onClick={handleCopyLink}
            >
              {copied ? (
                <>
                  <Check className="h-4 w-4 mr-2" />
                  Copied!
                </>
              ) : (
                <>
                  <Copy className="h-4 w-4 mr-2" />
                  Copy Link
                </>
              )}
            </Button>
            <Button
              variant="outline"
              className="flex-1"
              onClick={handleDownloadQr}
            >
              <Download className="h-4 w-4 mr-2" />
              Download QR
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
