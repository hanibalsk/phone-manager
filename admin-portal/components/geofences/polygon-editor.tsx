"use client";

import { useState, useMemo } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Plus, Trash2, GripVertical, ChevronUp, ChevronDown } from "lucide-react";

interface Coordinate {
  latitude: number;
  longitude: number;
}

interface PolygonEditorProps {
  coordinates: Coordinate[];
  onChange: (coordinates: Coordinate[]) => void;
  errors?: string;
}

export function PolygonEditor({ coordinates, onChange, errors }: PolygonEditorProps) {
  const [newLat, setNewLat] = useState("");
  const [newLng, setNewLng] = useState("");
  const [inputError, setInputError] = useState("");

  const addCoordinate = () => {
    setInputError("");

    const lat = parseFloat(newLat);
    const lng = parseFloat(newLng);

    if (isNaN(lat) || lat < -90 || lat > 90) {
      setInputError("Latitude must be between -90 and 90");
      return;
    }

    if (isNaN(lng) || lng < -180 || lng > 180) {
      setInputError("Longitude must be between -180 and 180");
      return;
    }

    onChange([...coordinates, { latitude: lat, longitude: lng }]);
    setNewLat("");
    setNewLng("");
  };

  const removeCoordinate = (index: number) => {
    onChange(coordinates.filter((_, i) => i !== index));
  };

  const moveCoordinate = (index: number, direction: "up" | "down") => {
    const newCoords = [...coordinates];
    const targetIndex = direction === "up" ? index - 1 : index + 1;

    if (targetIndex < 0 || targetIndex >= coordinates.length) return;

    [newCoords[index], newCoords[targetIndex]] = [newCoords[targetIndex], newCoords[index]];
    onChange(newCoords);
  };

  const updateCoordinate = (index: number, field: "latitude" | "longitude", value: string) => {
    const numValue = parseFloat(value);
    if (isNaN(numValue)) return;

    const newCoords = [...coordinates];
    newCoords[index] = { ...newCoords[index], [field]: numValue };
    onChange(newCoords);
  };

  // Calculate bounds and polygon points for preview
  const previewData = useMemo(() => {
    if (coordinates.length < 2) return null;

    const lats = coordinates.map(c => c.latitude);
    const lngs = coordinates.map(c => c.longitude);

    const minLat = Math.min(...lats);
    const maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs);
    const maxLng = Math.max(...lngs);

    const padding = 0.15;
    const latRange = (maxLat - minLat) || 0.01;
    const lngRange = (maxLng - minLng) || 0.01;

    const points = coordinates.map(coord => {
      const x = ((coord.longitude - minLng + lngRange * padding) / (lngRange * (1 + padding * 2))) * 100;
      const y = ((maxLat + latRange * padding - coord.latitude) / (latRange * (1 + padding * 2))) * 100;
      return { x: Math.max(5, Math.min(95, x)), y: Math.max(5, Math.min(95, y)) };
    });

    return {
      points,
      svgPath: points.map((p, i) => `${i === 0 ? "M" : "L"} ${p.x} ${p.y}`).join(" ") + " Z",
    };
  }, [coordinates]);

  return (
    <div className="space-y-4">
      {/* Polygon Preview */}
      <div className="relative w-full h-48 bg-slate-100 dark:bg-slate-800 rounded-lg overflow-hidden border">
        {coordinates.length < 3 ? (
          <div className="absolute inset-0 flex items-center justify-center text-muted-foreground text-sm">
            Add at least 3 points to see polygon preview
          </div>
        ) : previewData ? (
          <svg viewBox="0 0 100 100" className="w-full h-full" preserveAspectRatio="xMidYMid meet">
            {/* Polygon fill */}
            <path
              d={previewData.svgPath}
              fill="rgba(59, 130, 246, 0.2)"
              stroke="#3b82f6"
              strokeWidth="0.5"
            />
            {/* Vertex points */}
            {previewData.points.map((point, index) => (
              <g key={index}>
                <circle
                  cx={point.x}
                  cy={point.y}
                  r="2"
                  fill="#3b82f6"
                  stroke="white"
                  strokeWidth="0.5"
                />
                <text
                  x={point.x + 3}
                  y={point.y - 3}
                  fontSize="4"
                  fill="#3b82f6"
                  className="select-none"
                >
                  {index + 1}
                </text>
              </g>
            ))}
          </svg>
        ) : null}

        {/* Coordinate count badge */}
        <div className="absolute top-2 right-2 bg-background/80 px-2 py-1 rounded text-xs">
          {coordinates.length} point{coordinates.length !== 1 ? "s" : ""}
        </div>
      </div>

      {/* Add Coordinate Form */}
      <div className="border rounded-lg p-4 space-y-3">
        <Label className="text-sm font-medium">Add New Point</Label>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <Input
              type="number"
              step="any"
              placeholder="Latitude (e.g., 37.7749)"
              value={newLat}
              onChange={(e) => setNewLat(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addCoordinate()}
            />
          </div>
          <div>
            <Input
              type="number"
              step="any"
              placeholder="Longitude (e.g., -122.4194)"
              value={newLng}
              onChange={(e) => setNewLng(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addCoordinate()}
            />
          </div>
        </div>
        {inputError && <p className="text-sm text-destructive">{inputError}</p>}
        <Button type="button" variant="outline" size="sm" onClick={addCoordinate}>
          <Plus className="h-4 w-4 mr-2" />
          Add Point
        </Button>
      </div>

      {/* Coordinates List */}
      {coordinates.length > 0 && (
        <div className="border rounded-lg divide-y">
          <div className="px-4 py-2 bg-muted/50 text-sm font-medium grid grid-cols-[auto_1fr_1fr_auto] gap-2 items-center">
            <span className="w-8">#</span>
            <span>Latitude</span>
            <span>Longitude</span>
            <span className="w-24">Actions</span>
          </div>
          {coordinates.map((coord, index) => (
            <div
              key={index}
              className="px-4 py-2 grid grid-cols-[auto_1fr_1fr_auto] gap-2 items-center hover:bg-muted/30"
            >
              <span className="w-8 text-sm text-muted-foreground flex items-center gap-1">
                <GripVertical className="h-3 w-3" />
                {index + 1}
              </span>
              <Input
                type="number"
                step="any"
                value={coord.latitude}
                onChange={(e) => updateCoordinate(index, "latitude", e.target.value)}
                className="h-8 text-sm"
              />
              <Input
                type="number"
                step="any"
                value={coord.longitude}
                onChange={(e) => updateCoordinate(index, "longitude", e.target.value)}
                className="h-8 text-sm"
              />
              <div className="w-24 flex items-center gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => moveCoordinate(index, "up")}
                  disabled={index === 0}
                >
                  <ChevronUp className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => moveCoordinate(index, "down")}
                  disabled={index === coordinates.length - 1}
                >
                  <ChevronDown className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-destructive hover:text-destructive"
                  onClick={() => removeCoordinate(index)}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Validation messages */}
      {errors && <p className="text-sm text-destructive">{errors}</p>}
      {coordinates.length > 0 && coordinates.length < 3 && (
        <p className="text-sm text-amber-600 dark:text-amber-400">
          A polygon requires at least 3 points
        </p>
      )}
    </div>
  );
}
