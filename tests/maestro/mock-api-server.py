#!/usr/bin/env python3
"""
Minimal mock API server for Maestro E2E tests.

This server provides mock responses for the Phone Manager API endpoints
that are called during E2E testing. It allows the app to complete
the registration flow and show the home screen.

Usage:
    python3 mock-api-server.py [port]
    Default port: 8080
"""

import json
import sys
import uuid
from datetime import datetime, timezone
from http.server import HTTPServer, BaseHTTPRequestHandler


class MockAPIHandler(BaseHTTPRequestHandler):
    """Handler for mock API requests."""

    def _send_json_response(self, status_code: int, data: dict):
        """Send a JSON response."""
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-Key")
        self.end_headers()
        self.wfile.write(json.dumps(data).encode())

    def _read_json_body(self) -> dict:
        """Read and parse JSON request body."""
        content_length = int(self.headers.get("Content-Length", 0))
        if content_length > 0:
            body = self.rfile.read(content_length)
            return json.loads(body.decode())
        return {}

    def do_OPTIONS(self):
        """Handle CORS preflight requests."""
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-API-Key")
        self.end_headers()

    def do_GET(self):
        """Handle GET requests."""
        path = self.path.split("?")[0]  # Remove query params

        # Health check
        if path == "/health" or path == "/":
            self._send_json_response(200, {"status": "healthy", "mock": True})
            return

        # Get devices in group
        if path.startswith("/api/v1/devices"):
            self._send_json_response(200, {
                "devices": [
                    {
                        "device_id": "test-device-001",
                        "display_name": "TestDevice",
                        "last_location": {
                            "latitude": 37.7749,
                            "longitude": -122.4194,
                            "timestamp": datetime.now(timezone.utc).isoformat()
                        },
                        "last_seen_at": datetime.now(timezone.utc).isoformat()
                    }
                ]
            })
            return

        # Get groups
        if path.startswith("/api/v1/groups"):
            self._send_json_response(200, {
                "groups": [
                    {
                        "id": "test-group",
                        "name": "Test Group",
                        "member_count": 1
                    }
                ]
            })
            return

        # Get geofences
        if path.startswith("/api/v1/geofences"):
            self._send_json_response(200, {"geofences": []})
            return

        # Public config
        if path == "/api/v1/config/public":
            self._send_json_response(200, {
                "features": {
                    "location_tracking": True,
                    "geofences": True,
                    "groups": True
                },
                "version": "1.0.0"
            })
            return

        # Default: return empty success
        self._send_json_response(200, {})

    def do_POST(self):
        """Handle POST requests."""
        path = self.path.split("?")[0]

        # Device registration
        if path == "/api/v1/devices/register":
            body = self._read_json_body()
            now = datetime.now(timezone.utc).isoformat()

            # Generate a device ID if not provided
            device_id = body.get("device_id", str(uuid.uuid4()))
            display_name = body.get("display_name", "TestDevice")
            group_id = body.get("group_id", "test-group")

            self._send_json_response(201, {
                "device_id": device_id,
                "display_name": display_name,
                "group_id": group_id,
                "created_at": now,
                "updated_at": now
            })
            return

        # Location upload
        if path == "/api/v1/locations" or path == "/api/v1/locations/batch":
            self._send_json_response(200, {"status": "ok", "processed": 1})
            return

        # Movement events
        if path.startswith("/api/v1/movement-events"):
            self._send_json_response(200, {"status": "ok"})
            return

        # Geofence events
        if path.startswith("/api/v1/geofence-events"):
            self._send_json_response(200, {"status": "ok"})
            return

        # Group operations
        if path.startswith("/api/v1/groups"):
            self._send_json_response(200, {
                "group": {
                    "id": "test-group",
                    "name": "Test Group",
                    "member_count": 1
                }
            })
            return

        # Default: return success
        self._send_json_response(200, {"status": "ok"})

    def do_PUT(self):
        """Handle PUT requests."""
        self._send_json_response(200, {"status": "ok"})

    def do_PATCH(self):
        """Handle PATCH requests."""
        self._send_json_response(200, {"status": "ok"})

    def do_DELETE(self):
        """Handle DELETE requests."""
        self._send_json_response(200, {"status": "ok"})

    def log_message(self, format, *args):
        """Log HTTP requests."""
        print(f"[MockAPI] {self.address_string()} - {format % args}")


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    server = HTTPServer(("0.0.0.0", port), MockAPIHandler)
    print(f"[MockAPI] Starting mock API server on port {port}")
    print(f"[MockAPI] Endpoints available:")
    print(f"  - POST /api/v1/devices/register")
    print(f"  - GET /api/v1/devices")
    print(f"  - POST /api/v1/locations")
    print(f"  - GET /health")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n[MockAPI] Shutting down...")
        server.shutdown()


if __name__ == "__main__":
    main()
