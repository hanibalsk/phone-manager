# Story AP-2.5: View Organization Statistics

## Story
As a super admin, I want to view usage statistics for each organization, so that I can monitor their activity and resource consumption.

## Status
Implemented

## Acceptance Criteria
1. ✅ Can view organization statistics (users, devices, groups, storage)
2. ✅ Shows current usage vs. limits
3. ✅ Displays usage trends over time
4. ✅ Statistics are refreshable
5. ✅ Accessible from organization actions menu

## Implementation Notes

### Components Created
- `OrganizationStatsDialog` - Modal dialog for viewing organization statistics

### API Endpoints Used
- `GET /api/admin/organizations/:id/stats` - Get organization statistics

### Data Structure
```typescript
interface OrganizationStats {
  users_count: number;
  devices_count: number;
  groups_count: number;
  storage_used_mb: number;
  usage_trends: {
    period: string;
    users: number;
    devices: number;
  }[];
}
```

### Statistics Displayed
| Metric | Description |
|--------|-------------|
| Users | Current count vs. max limit |
| Devices | Current count vs. max limit |
| Groups | Current count vs. max limit |
| Storage | Storage used in MB/GB |
| Trends | Historical data by period |

## Tasks
- [x] Create OrganizationStatsDialog component
- [x] Display summary statistics with icons
- [x] Show usage vs. limits comparison
- [x] Display usage trends table
- [x] Add refresh functionality
- [x] Add "View Statistics" option to actions menu
- [x] Integrate with organizationsApi.getStats
