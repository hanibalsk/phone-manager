"use client";

import { useState, useCallback, useEffect, useRef, useMemo } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  Square,
  Trash2,
  MapPin,
  ChevronDown,
  ChevronUp,
  ZoomIn,
  ZoomOut,
} from "lucide-react";

export interface GeoBounds {
  north: number;
  south: number;
  east: number;
  west: number;
}

interface BoundsSelectorProps {
  value: GeoBounds | null;
  onChange: (bounds: GeoBounds | null) => void;
  className?: string;
}

// Default world view
const DEFAULT_VIEW = {
  centerLat: 39.8283,
  centerLng: -98.5795,
  zoom: 1,
};

export function BoundsSelector({ value, onChange, className = "" }: BoundsSelectorProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [isDrawing, setIsDrawing] = useState(false);
  const [drawStart, setDrawStart] = useState<{ x: number; y: number } | null>(null);
  const [drawEnd, setDrawEnd] = useState<{ x: number; y: number } | null>(null);
  const [viewState, setViewState] = useState(DEFAULT_VIEW);
  const svgRef = useRef<SVGSVGElement>(null);

  // Manual input state
  const [manualNorth, setManualNorth] = useState(value?.north?.toString() || "");
  const [manualSouth, setManualSouth] = useState(value?.south?.toString() || "");
  const [manualEast, setManualEast] = useState(value?.east?.toString() || "");
  const [manualWest, setManualWest] = useState(value?.west?.toString() || "");

  // Update manual inputs when value changes
  useEffect(() => {
    if (value) {
      setManualNorth(value.north.toFixed(6));
      setManualSouth(value.south.toFixed(6));
      setManualEast(value.east.toFixed(6));
      setManualWest(value.west.toFixed(6));
    } else {
      setManualNorth("");
      setManualSouth("");
      setManualEast("");
      setManualWest("");
    }
  }, [value]);

  // Map projection helpers
  const getViewBounds = useMemo(() => {
    const latRange = 180 / viewState.zoom;
    const lngRange = 360 / viewState.zoom;
    return {
      minLat: Math.max(-90, viewState.centerLat - latRange / 2),
      maxLat: Math.min(90, viewState.centerLat + latRange / 2),
      minLng: Math.max(-180, viewState.centerLng - lngRange / 2),
      maxLng: Math.min(180, viewState.centerLng + lngRange / 2),
    };
  }, [viewState]);

  const latLngToXY = useCallback((lat: number, lng: number) => {
    const { minLat, maxLat, minLng, maxLng } = getViewBounds;
    const x = ((lng - minLng) / (maxLng - minLng)) * 100;
    const y = ((maxLat - lat) / (maxLat - minLat)) * 100;
    return { x, y };
  }, [getViewBounds]);

  const xyToLatLng = useCallback((x: number, y: number) => {
    const { minLat, maxLat, minLng, maxLng } = getViewBounds;
    const lng = minLng + (x / 100) * (maxLng - minLng);
    const lat = maxLat - (y / 100) * (maxLat - minLat);
    return { lat, lng };
  }, [getViewBounds]);

  // Calculate selected bounds rectangle
  const boundsRect = useMemo(() => {
    if (!value) return null;
    const topLeft = latLngToXY(value.north, value.west);
    const bottomRight = latLngToXY(value.south, value.east);
    return {
      x: topLeft.x,
      y: topLeft.y,
      width: bottomRight.x - topLeft.x,
      height: bottomRight.y - topLeft.y,
    };
  }, [value, latLngToXY]);

  // Calculate drawing rectangle
  const drawRect = useMemo(() => {
    if (!drawStart || !drawEnd) return null;
    return {
      x: Math.min(drawStart.x, drawEnd.x),
      y: Math.min(drawStart.y, drawEnd.y),
      width: Math.abs(drawEnd.x - drawStart.x),
      height: Math.abs(drawEnd.y - drawStart.y),
    };
  }, [drawStart, drawEnd]);

  const handleMouseDown = useCallback((e: React.MouseEvent<SVGSVGElement>) => {
    if (!isDrawing || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;
    setDrawStart({ x, y });
    setDrawEnd({ x, y });
  }, [isDrawing]);

  const handleMouseMove = useCallback((e: React.MouseEvent<SVGSVGElement>) => {
    if (!isDrawing || !drawStart || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    const x = Math.max(0, Math.min(100, ((e.clientX - rect.left) / rect.width) * 100));
    const y = Math.max(0, Math.min(100, ((e.clientY - rect.top) / rect.height) * 100));
    setDrawEnd({ x, y });
  }, [isDrawing, drawStart]);

  const handleMouseUp = useCallback(() => {
    if (!isDrawing || !drawStart || !drawEnd) return;

    // Convert pixel coordinates to lat/lng
    const topLeft = xyToLatLng(Math.min(drawStart.x, drawEnd.x), Math.min(drawStart.y, drawEnd.y));
    const bottomRight = xyToLatLng(Math.max(drawStart.x, drawEnd.x), Math.max(drawStart.y, drawEnd.y));

    // Minimum area check
    const latDiff = Math.abs(topLeft.lat - bottomRight.lat);
    const lngDiff = Math.abs(topLeft.lng - bottomRight.lng);

    if (latDiff > 0.1 && lngDiff > 0.1) {
      onChange({
        north: topLeft.lat,
        south: bottomRight.lat,
        east: bottomRight.lng,
        west: topLeft.lng,
      });
    }

    setDrawStart(null);
    setDrawEnd(null);
    setIsDrawing(false);
  }, [isDrawing, drawStart, drawEnd, xyToLatLng, onChange]);

  const handleClear = useCallback(() => {
    onChange(null);
  }, [onChange]);

  const handleZoomIn = useCallback(() => {
    setViewState(prev => ({
      ...prev,
      zoom: Math.min(prev.zoom * 2, 16),
    }));
  }, []);

  const handleZoomOut = useCallback(() => {
    setViewState(prev => ({
      ...prev,
      zoom: Math.max(prev.zoom / 2, 0.5),
    }));
  }, []);

  const handleManualApply = useCallback(() => {
    const north = parseFloat(manualNorth);
    const south = parseFloat(manualSouth);
    const east = parseFloat(manualEast);
    const west = parseFloat(manualWest);

    if (
      !isNaN(north) && !isNaN(south) && !isNaN(east) && !isNaN(west) &&
      north > south && east > west &&
      north >= -90 && north <= 90 &&
      south >= -90 && south <= 90 &&
      east >= -180 && east <= 180 &&
      west >= -180 && west <= 180
    ) {
      onChange({ north, south, east, west });
      // Center view on the bounds
      setViewState({
        centerLat: (north + south) / 2,
        centerLng: (east + west) / 2,
        zoom: Math.min(
          180 / Math.abs(north - south),
          360 / Math.abs(east - west)
        ) * 0.8,
      });
    }
  }, [manualNorth, manualSouth, manualEast, manualWest, onChange]);

  // Generate grid lines
  const gridLines = useMemo(() => {
    const { minLat, maxLat, minLng, maxLng } = getViewBounds;
    const lines: { x1: number; y1: number; x2: number; y2: number; label?: string }[] = [];

    // Latitude lines
    const latStep = (maxLat - minLat) / 4;
    for (let lat = Math.ceil(minLat / latStep) * latStep; lat <= maxLat; lat += latStep) {
      const pos = latLngToXY(lat, minLng);
      lines.push({ x1: 0, y1: pos.y, x2: 100, y2: pos.y, label: lat.toFixed(0) + "°" });
    }

    // Longitude lines
    const lngStep = (maxLng - minLng) / 4;
    for (let lng = Math.ceil(minLng / lngStep) * lngStep; lng <= maxLng; lng += lngStep) {
      const pos = latLngToXY(maxLat, lng);
      lines.push({ x1: pos.x, y1: 0, x2: pos.x, y2: 100, label: lng.toFixed(0) + "°" });
    }

    return lines;
  }, [getViewBounds, latLngToXY]);

  return (
    <div className={`space-y-2 ${className}`}>
      <div className="flex items-center justify-between">
        <Label className="flex items-center gap-2">
          <MapPin className="h-4 w-4" />
          Geographic Bounds (Optional)
        </Label>
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setIsExpanded(!isExpanded)}
          className="h-7 px-2"
        >
          {isExpanded ? (
            <ChevronUp className="h-4 w-4" />
          ) : (
            <ChevronDown className="h-4 w-4" />
          )}
        </Button>
      </div>

      {/* Collapsed state shows current bounds */}
      {!isExpanded && value && (
        <div className="text-xs text-muted-foreground p-2 bg-muted rounded-md flex items-center justify-between">
          <span>
            Bounds: {value.south.toFixed(4)}°, {value.west.toFixed(4)}° to{" "}
            {value.north.toFixed(4)}°, {value.east.toFixed(4)}°
          </span>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleClear}
            className="h-6 px-2"
          >
            <Trash2 className="h-3 w-3" />
          </Button>
        </div>
      )}

      {isExpanded && (
        <div className="border rounded-lg overflow-hidden">
          {/* Map for drawing */}
          <div className="relative">
            <svg
              ref={svgRef}
              viewBox="0 0 100 100"
              className="w-full h-[200px] bg-blue-50 dark:bg-slate-800"
              style={{ cursor: isDrawing ? "crosshair" : "default" }}
              onMouseDown={handleMouseDown}
              onMouseMove={handleMouseMove}
              onMouseUp={handleMouseUp}
              onMouseLeave={handleMouseUp}
            >
              {/* Grid lines */}
              {gridLines.map((line, i) => (
                <g key={i}>
                  <line
                    x1={`${line.x1}%`}
                    y1={`${line.y1}%`}
                    x2={`${line.x2}%`}
                    y2={`${line.y2}%`}
                    stroke="currentColor"
                    strokeOpacity={0.2}
                    strokeWidth={0.5}
                  />
                  {line.label && (
                    <text
                      x={line.x1 === line.x2 ? `${line.x1}%` : "2%"}
                      y={line.y1 === line.y2 ? `${line.y1}%` : "98%"}
                      fontSize="3"
                      fill="currentColor"
                      opacity={0.5}
                    >
                      {line.label}
                    </text>
                  )}
                </g>
              ))}

              {/* Selected bounds rectangle */}
              {boundsRect && (
                <rect
                  x={`${boundsRect.x}%`}
                  y={`${boundsRect.y}%`}
                  width={`${boundsRect.width}%`}
                  height={`${boundsRect.height}%`}
                  fill="rgb(59, 130, 246)"
                  fillOpacity={0.2}
                  stroke="rgb(59, 130, 246)"
                  strokeWidth={1}
                />
              )}

              {/* Drawing rectangle */}
              {drawRect && (
                <rect
                  x={`${drawRect.x}%`}
                  y={`${drawRect.y}%`}
                  width={`${drawRect.width}%`}
                  height={`${drawRect.height}%`}
                  fill="rgb(245, 158, 11)"
                  fillOpacity={0.3}
                  stroke="rgb(245, 158, 11)"
                  strokeWidth={1}
                  strokeDasharray="2,2"
                />
              )}
            </svg>

            {/* Controls overlay */}
            <div className="absolute top-2 right-2 flex flex-col gap-1">
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={handleZoomIn}
                className="h-8 w-8 p-0"
              >
                <ZoomIn className="h-4 w-4" />
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="sm"
                onClick={handleZoomOut}
                className="h-8 w-8 p-0"
              >
                <ZoomOut className="h-4 w-4" />
              </Button>
            </div>

            <div className="absolute top-2 left-2 flex gap-1">
              <Button
                type="button"
                variant={isDrawing ? "default" : "secondary"}
                size="sm"
                onClick={() => setIsDrawing(!isDrawing)}
                className="h-8"
              >
                <Square className="h-4 w-4 mr-1" />
                {isDrawing ? "Drawing..." : "Draw Area"}
              </Button>
              {value && (
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={handleClear}
                  className="h-8 bg-background"
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              )}
            </div>

            {/* Drawing instructions */}
            {isDrawing && (
              <div className="absolute bottom-2 left-2 right-2 text-xs text-center bg-amber-100 dark:bg-amber-900/50 text-amber-800 dark:text-amber-200 p-2 rounded">
                Click and drag to draw a rectangle
              </div>
            )}
          </div>

          {/* Manual coordinate input */}
          <div className="p-3 border-t bg-muted/30">
            <p className="text-xs text-muted-foreground mb-2">
              Or enter coordinates manually:
            </p>
            <div className="grid grid-cols-4 gap-2">
              <div>
                <Label className="text-xs">North</Label>
                <Input
                  type="number"
                  step="0.0001"
                  placeholder="90"
                  value={manualNorth}
                  onChange={(e) => setManualNorth(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
              <div>
                <Label className="text-xs">South</Label>
                <Input
                  type="number"
                  step="0.0001"
                  placeholder="-90"
                  value={manualSouth}
                  onChange={(e) => setManualSouth(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
              <div>
                <Label className="text-xs">East</Label>
                <Input
                  type="number"
                  step="0.0001"
                  placeholder="180"
                  value={manualEast}
                  onChange={(e) => setManualEast(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
              <div>
                <Label className="text-xs">West</Label>
                <Input
                  type="number"
                  step="0.0001"
                  placeholder="-180"
                  value={manualWest}
                  onChange={(e) => setManualWest(e.target.value)}
                  className="h-8 text-xs"
                />
              </div>
            </div>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={handleManualApply}
              className="mt-2 w-full h-7 text-xs"
              disabled={!manualNorth || !manualSouth || !manualEast || !manualWest}
            >
              Apply Coordinates
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
