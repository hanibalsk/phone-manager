# Story AP-2.4: Enable/Disable Features per Organization

## Story
As a super admin, I want to enable/disable specific features for each organization, so that I can customize their capabilities based on their plan.

## Status
Implemented

## Acceptance Criteria
1. ✅ Can toggle features on/off for organization
2. ✅ Features displayed with descriptions
3. ✅ Changes saved via API call
4. ✅ Current feature states shown on load
5. ✅ Accessible from organization actions menu

## Implementation Notes

### Components Created
- `OrganizationFeaturesDialog` - Modal for managing feature flags

### API Endpoints Used
- `PUT /api/admin/organizations/:id/features` - Update organization features

### Available Features
| Feature | Description |
|---------|-------------|
| geofences | Enable geofence-based location tracking and alerts |
| proximity_alerts | Receive alerts when devices enter/leave areas |
| webhooks | Allow integration with external services |
| trips | Enable trip tracking and history |
| movement_tracking | Track device movement patterns |

### Data Structure
```typescript
interface OrganizationFeatures {
  geofences: boolean;
  proximity_alerts: boolean;
  webhooks: boolean;
  trips: boolean;
  movement_tracking: boolean;
}
```

## Tasks
- [x] Create OrganizationFeaturesDialog component
- [x] Add toggle switches for each feature
- [x] Display feature descriptions
- [x] Integrate with organizationsApi.updateFeatures
- [x] Add "Manage Features" option to actions menu
