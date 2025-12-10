"use client";

import { useState } from "react";
import type { DeviceLocation } from "@/types";
import { exportToCSV, exportToJSON, exportToGPX } from "@/lib/export-utils";
import { Button } from "@/components/ui/button";
import { Download, ChevronDown } from "lucide-react";

interface ExportDropdownProps {
  locations: DeviceLocation[];
  disabled?: boolean;
}

export function ExportDropdown({ locations, disabled }: ExportDropdownProps) {
  const [isOpen, setIsOpen] = useState(false);

  const handleExport = (format: "csv" | "json" | "gpx") => {
    const filename = `location-history-${new Date().toISOString().split("T")[0]}`;

    switch (format) {
      case "csv":
        exportToCSV(locations, filename);
        break;
      case "json":
        exportToJSON(locations, filename);
        break;
      case "gpx":
        exportToGPX(locations, filename);
        break;
    }

    setIsOpen(false);
  };

  return (
    <div className="relative">
      <Button
        variant="outline"
        disabled={disabled || locations.length === 0}
        onClick={() => setIsOpen(!isOpen)}
      >
        <Download className="h-4 w-4 mr-2" />
        Export
        <ChevronDown className="h-4 w-4 ml-2" />
      </Button>

      {isOpen && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />
          <div className="absolute right-0 mt-2 w-48 bg-background border rounded-md shadow-lg z-20">
            <div className="py-1">
              <button
                className="w-full px-4 py-2 text-left text-sm hover:bg-muted flex items-center justify-between"
                onClick={() => handleExport("csv")}
              >
                <span>Export as CSV</span>
                <span className="text-xs text-muted-foreground">.csv</span>
              </button>
              <button
                className="w-full px-4 py-2 text-left text-sm hover:bg-muted flex items-center justify-between"
                onClick={() => handleExport("json")}
              >
                <span>Export as JSON</span>
                <span className="text-xs text-muted-foreground">.json</span>
              </button>
              <button
                className="w-full px-4 py-2 text-left text-sm hover:bg-muted flex items-center justify-between"
                onClick={() => handleExport("gpx")}
              >
                <span>Export as GPX</span>
                <span className="text-xs text-muted-foreground">.gpx</span>
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
