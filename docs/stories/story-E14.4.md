# Story E14.4: App Usage Analytics

**Story ID**: E14.4
**Epic**: 14 - Admin Web Portal
**Priority**: Medium
**Estimate**: 2 story points
**Status**: Complete
**Created**: 2025-12-02
**Dependencies**: E14.1, E14.2

---

## Story

As an administrator,
I want to view app usage analytics for each device,
so that I can understand how the device is being used.

## Acceptance Criteria

### AC E14.4.1: Usage Summary
- Display total screen time
- Show number of apps used
- Show average time per app

### AC E14.4.2: Usage Chart
- Visual bar chart of app usage
- Sorted by time spent
- Top 10 apps displayed

### AC E14.4.3: Date Selection
- Ability to select different dates
- Refresh data functionality

## Tasks / Subtasks

- [x] Task 1: Create Usage Page Route
- [x] Task 2: Implement Usage Summary Cards
- [x] Task 3: Create Usage Bar Chart
- [x] Task 4: Add Date Picker
- [x] Task 5: Connect to API

## File List

### Created Files

- `admin-portal/app/devices/[id]/usage/page.tsx` - Device usage page
- `admin-portal/components/usage/usage-chart.tsx` - Usage bar chart
- `admin-portal/components/usage/usage-summary.tsx` - Summary cards
- `admin-portal/components/usage/index.tsx` - Component exports

---

**Last Updated**: 2025-12-02
**Status**: Complete
