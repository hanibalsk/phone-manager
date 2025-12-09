# Story AP-2.2: Configure Organization Limits

## Story
As a super admin, I want to configure organization-specific limits (users, devices, storage), so that I can manage resource allocation for each organization.

## Status
Implemented

## Acceptance Criteria
1. ✅ Can edit organization limits (max users, max devices, max groups)
2. ✅ Limits are validated (positive numbers required)
3. ✅ Changes are saved via API call
4. ✅ Form shows current limit values
5. ✅ Accessible from organization actions menu

## Implementation Notes

### Components Created
- `OrganizationLimitsDialog` - Modal dialog for editing organization limits

### API Endpoints Used
- `PUT /api/admin/organizations/:id/limits` - Update organization limits

### Data Structure
```typescript
interface LimitsUpdate {
  max_users?: number;
  max_devices?: number;
  max_groups?: number;
}
```

## Tasks
- [x] Create OrganizationLimitsDialog component
- [x] Add form validation for limit values
- [x] Integrate with organizationsApi.updateLimits
- [x] Add "Configure Limits" option to actions menu
