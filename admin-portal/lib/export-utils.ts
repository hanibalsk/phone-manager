import type { DeviceLocation } from "@/types";

/**
 * Export location data to CSV format
 */
export function exportToCSV(locations: DeviceLocation[], filename: string = "locations"): void {
  const headers = [
    "Device ID",
    "Device Name",
    "Latitude",
    "Longitude",
    "Accuracy (m)",
    "Altitude (m)",
    "Speed (m/s)",
    "Bearing",
    "Battery (%)",
    "Timestamp",
    "Organization",
  ];

  const rows = locations.map((loc) => [
    loc.device_id,
    loc.device_name,
    loc.latitude.toFixed(6),
    loc.longitude.toFixed(6),
    loc.accuracy.toFixed(1),
    loc.altitude?.toFixed(1) ?? "",
    loc.speed?.toFixed(1) ?? "",
    loc.bearing?.toFixed(1) ?? "",
    loc.battery_level !== null ? (loc.battery_level * 100).toFixed(0) : "",
    loc.timestamp,
    loc.organization_name,
  ]);

  const csvContent = [
    headers.join(","),
    ...rows.map((row) =>
      row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(",")
    ),
  ].join("\n");

  downloadFile(csvContent, `${filename}.csv`, "text/csv");
}

/**
 * Export location data to JSON format
 */
export function exportToJSON(locations: DeviceLocation[], filename: string = "locations"): void {
  const jsonContent = JSON.stringify(locations, null, 2);
  downloadFile(jsonContent, `${filename}.json`, "application/json");
}

/**
 * Export location data to GPX format (GPS Exchange Format)
 */
export function exportToGPX(locations: DeviceLocation[], filename: string = "locations"): void {
  const gpxHeader = `<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1" creator="Phone Manager Admin Portal"
  xmlns="http://www.topografix.com/GPX/1/1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">
  <metadata>
    <name>Location Export</name>
    <time>${new Date().toISOString()}</time>
  </metadata>`;

  const waypoints = locations
    .map((loc) => {
      const ele = loc.altitude !== null ? `    <ele>${loc.altitude}</ele>\n` : "";
      return `  <wpt lat="${loc.latitude}" lon="${loc.longitude}">
${ele}    <time>${loc.timestamp}</time>
    <name>${escapeXml(loc.device_name)}</name>
    <desc>Device: ${escapeXml(loc.device_name)}, Accuracy: ${loc.accuracy}m</desc>
  </wpt>`;
    })
    .join("\n");

  // Group by device for tracks
  const deviceGroups = new Map<string, DeviceLocation[]>();
  for (const loc of locations) {
    const existing = deviceGroups.get(loc.device_id) || [];
    existing.push(loc);
    deviceGroups.set(loc.device_id, existing);
  }

  const tracks = Array.from(deviceGroups.entries())
    .map(([deviceId, locs]) => {
      const sortedLocs = [...locs].sort(
        (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
      );
      const trackPoints = sortedLocs
        .map((loc) => {
          const ele = loc.altitude !== null ? `        <ele>${loc.altitude}</ele>\n` : "";
          return `      <trkpt lat="${loc.latitude}" lon="${loc.longitude}">
${ele}        <time>${loc.timestamp}</time>
      </trkpt>`;
        })
        .join("\n");

      return `  <trk>
    <name>${escapeXml(sortedLocs[0]?.device_name || deviceId)}</name>
    <trkseg>
${trackPoints}
    </trkseg>
  </trk>`;
    })
    .join("\n");

  const gpxContent = `${gpxHeader}
${waypoints}
${tracks}
</gpx>`;

  downloadFile(gpxContent, `${filename}.gpx`, "application/gpx+xml");
}

/**
 * Helper to escape XML special characters
 */
function escapeXml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

/**
 * Helper to download a file
 */
function downloadFile(content: string, filename: string, mimeType: string): void {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
