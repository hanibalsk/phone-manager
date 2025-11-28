# Phone Manager API Reference

## Overview

Phone Manager communicates with a backend server for device registration, location uploads, group management, and feature synchronization. All endpoints require authentication via `X-API-Key` header.

**Backend Repository**: [phone-manager-backend](https://github.com/hanibalsk/phone-manager-backend)

## Base Configuration

| Setting | Value |
|---------|-------|
| Base URL | Configurable via `API_BASE_URL` |
| Content Type | `application/json` |
| Authentication | `X-API-Key` header |
| Timeout | 30 seconds |

## Authentication

All requests must include the API key header:

```http
X-API-Key: your-api-key-here
Content-Type: application/json
```

---

## Device Endpoints

### Register Device

Registers a new device with the server.

**Endpoint:** `POST /api/devices/register`

**Request:**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "displayName": "John's Phone",
  "groupId": "family-group"
}
```

**Response (201 Created):**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "displayName": "John's Phone",
  "groupId": "family-group",
  "createdAt": "2025-01-15T10:30:00Z"
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request body or validation error
- `409 Conflict`: Device already registered

### Get Group Members

Retrieves all devices in a group.

**Endpoint:** `GET /api/devices?groupId={groupId}`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| groupId | string | Yes | Group identifier |

**Response (200 OK):**
```json
{
  "devices": [
    {
      "deviceId": "550e8400-e29b-41d4-a716-446655440000",
      "displayName": "John's Phone",
      "lastLocation": {
        "latitude": 48.1486,
        "longitude": 17.1077,
        "accuracy": 10.5,
        "timestamp": "2025-01-15T10:30:00Z"
      },
      "lastSeenAt": "2025-01-15T10:30:00Z"
    }
  ],
  "total": 1
}
```

### Get Device History

Retrieves location history for a device.

**Endpoint:** `GET /api/devices/{deviceId}/locations`

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| deviceId | string | Yes | Device UUID |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| startDate | ISO8601 | No | 7 days ago | Start of date range |
| endDate | ISO8601 | No | Now | End of date range |
| limit | integer | No | 500 | Maximum records |

**Response (200 OK):**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "locations": [
    {
      "latitude": 48.1486,
      "longitude": 17.1077,
      "accuracy": 10.5,
      "altitude": 150.0,
      "speed": 0.0,
      "timestamp": "2025-01-15T10:30:00Z"
    }
  ],
  "total": 1
}
```

---

## Location Endpoints

### Upload Single Location

Uploads a single location record.

**Endpoint:** `POST /api/v1/locations`

**Request:**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "latitude": 48.1486,
  "longitude": 17.1077,
  "accuracy": 10.5,
  "altitude": 150.0,
  "speed": 0.0,
  "provider": "gps",
  "timestamp": "2025-01-15T10:30:00Z"
}
```

**Response (201 Created):**
```json
{
  "id": "loc-001",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "received": true
}
```

### Batch Upload Locations

Uploads multiple location records in a single request.

**Endpoint:** `POST /api/v1/locations/batch`

**Request:**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "locations": [
    {
      "latitude": 48.1486,
      "longitude": 17.1077,
      "accuracy": 10.5,
      "timestamp": "2025-01-15T10:30:00Z"
    },
    {
      "latitude": 48.1490,
      "longitude": 17.1080,
      "accuracy": 8.0,
      "timestamp": "2025-01-15T10:35:00Z"
    }
  ]
}
```

**Response (201 Created):**
```json
{
  "received": 2,
  "failed": 0
}
```

---

## Geofence Endpoints

### Create Geofence

Creates a new geofence definition.

**Endpoint:** `POST /api/v1/geofences`

**Request:**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Home",
  "latitude": 48.1486,
  "longitude": 17.1077,
  "radiusMeters": 100.0,
  "eventTypes": ["ENTER", "EXIT"],
  "active": true
}
```

**Response (201 Created):**
```json
{
  "geofenceId": "geo-001",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Home",
  "latitude": 48.1486,
  "longitude": 17.1077,
  "radiusMeters": 100.0,
  "eventTypes": ["ENTER", "EXIT"],
  "active": true,
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

### List Geofences

Retrieves all geofences for a device.

**Endpoint:** `GET /api/v1/geofences`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| deviceId | string | Yes | Device UUID |
| includeInactive | boolean | No | Include inactive geofences |

**Response (200 OK):**
```json
{
  "geofences": [
    {
      "geofenceId": "geo-001",
      "deviceId": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Home",
      "latitude": 48.1486,
      "longitude": 17.1077,
      "radiusMeters": 100.0,
      "eventTypes": ["ENTER", "EXIT"],
      "active": true,
      "metadata": {
        "webhookId": "webhook-001"
      },
      "createdAt": "2025-01-15T10:30:00Z",
      "updatedAt": "2025-01-15T10:30:00Z"
    }
  ],
  "total": 1
}
```

### Update Geofence

Updates an existing geofence.

**Endpoint:** `PUT /api/v1/geofences/{geofenceId}`

**Request:**
```json
{
  "name": "Home Updated",
  "radiusMeters": 150.0,
  "active": true
}
```

**Response (200 OK):**
Returns the updated geofence object.

### Delete Geofence

Deletes a geofence.

**Endpoint:** `DELETE /api/v1/geofences/{geofenceId}`

**Response (204 No Content)**

---

## Geofence Event Endpoints

### Report Geofence Event

Reports a geofence transition event.

**Endpoint:** `POST /api/v1/geofence-events`

**Request:**
```json
{
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "geofenceId": "geo-001",
  "eventType": "ENTER",
  "timestamp": "2025-01-15T10:30:00Z",
  "latitude": 48.1486,
  "longitude": 17.1077
}
```

**Response (201 Created):**
```json
{
  "eventId": "evt-001",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "geofenceId": "geo-001",
  "eventType": "ENTER",
  "timestamp": "2025-01-15T10:30:00Z",
  "webhookDelivered": true,
  "webhookResponseCode": 200
}
```

### List Geofence Events

Retrieves geofence events.

**Endpoint:** `GET /api/v1/geofence-events`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| deviceId | string | Yes | Device UUID |
| geofenceId | string | No | Filter by geofence |
| startDate | ISO8601 | No | Start of date range |
| endDate | ISO8601 | No | End of date range |
| limit | integer | No | Maximum records |

---

## Webhook Endpoints

### Create Webhook

Creates a new webhook configuration.

**Endpoint:** `POST /api/v1/webhooks`

**Request:**
```json
{
  "ownerDeviceId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Home Assistant",
  "targetUrl": "https://homeassistant.local/api/webhook/phone-manager",
  "secret": "generated-hmac-secret",
  "enabled": true
}
```

**Response (201 Created):**
```json
{
  "webhookId": "webhook-001",
  "ownerDeviceId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Home Assistant",
  "targetUrl": "https://homeassistant.local/api/webhook/phone-manager",
  "enabled": true,
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

### List Webhooks

Retrieves all webhooks for a device.

**Endpoint:** `GET /api/v1/webhooks`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ownerDeviceId | string | Yes | Device UUID |

**Response (200 OK):**
```json
{
  "webhooks": [
    {
      "webhookId": "webhook-001",
      "ownerDeviceId": "550e8400-e29b-41d4-a716-446655440000",
      "name": "Home Assistant",
      "targetUrl": "https://homeassistant.local/api/webhook/phone-manager",
      "enabled": true,
      "createdAt": "2025-01-15T10:30:00Z",
      "updatedAt": "2025-01-15T10:30:00Z"
    }
  ],
  "total": 1
}
```

### Update Webhook

Updates an existing webhook.

**Endpoint:** `PUT /api/v1/webhooks/{webhookId}`

**Request:**
```json
{
  "name": "Home Assistant Updated",
  "enabled": false
}
```

### Delete Webhook

Deletes a webhook.

**Endpoint:** `DELETE /api/v1/webhooks/{webhookId}`

**Response (204 No Content)**

---

## Proximity Alert Endpoints

### Create Proximity Alert

Creates a new proximity alert.

**Endpoint:** `POST /api/v1/proximity-alerts`

**Request:**
```json
{
  "ownerDeviceId": "550e8400-e29b-41d4-a716-446655440000",
  "targetDeviceId": "660e8400-e29b-41d4-a716-446655440001",
  "radiusMeters": 500,
  "direction": "ENTER",
  "active": true
}
```

**Response (201 Created):**
```json
{
  "alertId": "alert-001",
  "ownerDeviceId": "550e8400-e29b-41d4-a716-446655440000",
  "targetDeviceId": "660e8400-e29b-41d4-a716-446655440001",
  "radiusMeters": 500,
  "direction": "ENTER",
  "active": true,
  "lastState": "OUTSIDE",
  "createdAt": "2025-01-15T10:30:00Z",
  "updatedAt": "2025-01-15T10:30:00Z"
}
```

### List Proximity Alerts

Retrieves all proximity alerts for a device.

**Endpoint:** `GET /api/v1/proximity-alerts`

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| ownerDeviceId | string | Yes | Device UUID |

### Update Proximity Alert

**Endpoint:** `PUT /api/v1/proximity-alerts/{alertId}`

### Delete Proximity Alert

**Endpoint:** `DELETE /api/v1/proximity-alerts/{alertId}`

---

## Webhook Payload Format

When geofence events trigger webhooks, the backend sends the following payload:

**Webhook Request:**
```http
POST {targetUrl}
Content-Type: application/json
X-Signature: sha256=abc123...
```

**Payload:**
```json
{
  "event": "geofence_trigger",
  "deviceId": "550e8400-e29b-41d4-a716-446655440000",
  "deviceName": "John's Phone",
  "geofenceId": "geo-001",
  "geofenceName": "Home",
  "eventType": "ENTER",
  "timestamp": "2025-01-15T10:30:00Z",
  "location": {
    "latitude": 48.1486,
    "longitude": 17.1077
  }
}
```

### HMAC Signature Verification

The `X-Signature` header contains an HMAC-SHA256 signature of the payload:

```javascript
// Verification example (Node.js)
const crypto = require('crypto');

const payload = JSON.stringify(body);
const expectedSignature = crypto
  .createHmac('sha256', webhookSecret)
  .update(payload)
  .digest('hex');

const receivedSignature = headers['x-signature'].replace('sha256=', '');
const valid = crypto.timingSafeEqual(
  Buffer.from(expectedSignature),
  Buffer.from(receivedSignature)
);
```

---

## Error Responses

All endpoints return standardized error responses:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request body",
    "details": {
      "field": "radiusMeters",
      "reason": "Must be between 50 and 10000"
    }
  }
}
```

**Common Error Codes:**

| HTTP Status | Code | Description |
|-------------|------|-------------|
| 400 | VALIDATION_ERROR | Invalid request body |
| 401 | UNAUTHORIZED | Missing or invalid API key |
| 403 | FORBIDDEN | Access denied |
| 404 | NOT_FOUND | Resource not found |
| 409 | CONFLICT | Resource already exists |
| 429 | RATE_LIMITED | Too many requests |
| 500 | INTERNAL_ERROR | Server error |

---

## Rate Limiting

| Endpoint Type | Limit |
|---------------|-------|
| Location uploads | 1000/hour per device |
| Other endpoints | 100/minute per API key |

Rate limit headers are included in responses:
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705312200
```
