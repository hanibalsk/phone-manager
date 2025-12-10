"use client";

import type { TripPoint } from "@/types";
import { MapPin, Navigation } from "lucide-react";

interface TripMapProps {
  points: TripPoint[];
  startLabel?: string;
  endLabel?: string;
}

export function TripMap({ points, startLabel = "Start", endLabel = "End" }: TripMapProps) {
  if (points.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center bg-muted/30 rounded-lg">
        <MapPin className="h-12 w-12 text-muted-foreground mb-4" />
        <p className="text-muted-foreground">No path data available</p>
      </div>
    );
  }

  const startPoint = points[0];
  const endPoint = points[points.length - 1];

  // Calculate bounds
  const lats = points.map((p) => p.latitude);
  const lngs = points.map((p) => p.longitude);
  const minLat = Math.min(...lats);
  const maxLat = Math.max(...lats);
  const minLng = Math.min(...lngs);
  const maxLng = Math.max(...lngs);

  // Add padding
  const padding = 0.1;
  const latRange = maxLat - minLat || 0.01;
  const lngRange = maxLng - minLng || 0.01;

  // SVG dimensions
  const width = 600;
  const height = 400;
  const paddedMinLat = minLat - latRange * padding;
  const paddedMaxLat = maxLat + latRange * padding;
  const paddedMinLng = minLng - lngRange * padding;
  const paddedMaxLng = maxLng + lngRange * padding;

  // Transform coordinates to SVG space
  const toSvgX = (lng: number) =>
    ((lng - paddedMinLng) / (paddedMaxLng - paddedMinLng)) * width;
  const toSvgY = (lat: number) =>
    height - ((lat - paddedMinLat) / (paddedMaxLat - paddedMinLat)) * height;

  // Create path string
  const pathD = points
    .map((p, i) => {
      const x = toSvgX(p.longitude);
      const y = toSvgY(p.latitude);
      return `${i === 0 ? "M" : "L"} ${x} ${y}`;
    })
    .join(" ");

  return (
    <div className="relative bg-muted/30 rounded-lg overflow-hidden" data-testid="trip-map">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full h-auto"
        style={{ maxHeight: "400px" }}
      >
        {/* Grid lines */}
        <defs>
          <pattern
            id="grid"
            width="40"
            height="40"
            patternUnits="userSpaceOnUse"
          >
            <path
              d="M 40 0 L 0 0 0 40"
              fill="none"
              stroke="currentColor"
              strokeWidth="0.5"
              className="text-muted-foreground/20"
            />
          </pattern>
        </defs>
        <rect width={width} height={height} fill="url(#grid)" />

        {/* Trip path */}
        <path
          d={pathD}
          fill="none"
          stroke="currentColor"
          strokeWidth="3"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="text-primary"
        />

        {/* Speed-based gradient segments */}
        {points.slice(1).map((point, i) => {
          const prevPoint = points[i];
          const speed = point.speed || 0;
          const maxSpeed = Math.max(...points.map((p) => p.speed || 0)) || 1;
          const speedRatio = speed / maxSpeed;

          // Color from green (slow) to red (fast)
          const hue = 120 - speedRatio * 120;

          return (
            <line
              key={i}
              x1={toSvgX(prevPoint.longitude)}
              y1={toSvgY(prevPoint.latitude)}
              x2={toSvgX(point.longitude)}
              y2={toSvgY(point.latitude)}
              stroke={`hsl(${hue}, 70%, 50%)`}
              strokeWidth="3"
              strokeLinecap="round"
            />
          );
        })}

        {/* Start marker */}
        <g transform={`translate(${toSvgX(startPoint.longitude)}, ${toSvgY(startPoint.latitude)})`}>
          <circle r="12" className="fill-green-500" />
          <circle r="6" className="fill-white" />
        </g>

        {/* End marker */}
        <g transform={`translate(${toSvgX(endPoint.longitude)}, ${toSvgY(endPoint.latitude)})`}>
          <circle r="12" className="fill-red-500" />
          <circle r="6" className="fill-white" />
        </g>
      </svg>

      {/* Legend */}
      <div className="absolute bottom-2 left-2 flex gap-4 bg-background/80 backdrop-blur-sm rounded px-3 py-1.5 text-xs">
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded-full bg-green-500" />
          <span>{startLabel}</span>
        </div>
        <div className="flex items-center gap-1.5">
          <div className="w-3 h-3 rounded-full bg-red-500" />
          <span>{endLabel}</span>
        </div>
      </div>

      {/* Speed legend */}
      <div className="absolute bottom-2 right-2 flex items-center gap-2 bg-background/80 backdrop-blur-sm rounded px-3 py-1.5 text-xs">
        <span>Speed:</span>
        <div
          className="w-16 h-2 rounded"
          style={{
            background: "linear-gradient(to right, hsl(120, 70%, 50%), hsl(60, 70%, 50%), hsl(0, 70%, 50%))",
          }}
        />
        <Navigation className="h-3 w-3" />
      </div>
    </div>
  );
}
