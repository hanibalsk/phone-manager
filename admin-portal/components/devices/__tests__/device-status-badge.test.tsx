import { render, screen } from "@testing-library/react";
import { DeviceStatusBadge, calculateStatus } from "../device-status-badge";

describe("calculateStatus", () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("should return 'active' when last seen within 1 hour", () => {
    const now = new Date("2024-01-15T12:00:00Z");
    jest.setSystemTime(now);

    const lastSeen = new Date("2024-01-15T11:30:00Z").toISOString(); // 30 minutes ago
    expect(calculateStatus(lastSeen)).toBe("active");
  });

  it("should return 'inactive' when last seen between 1 and 24 hours", () => {
    const now = new Date("2024-01-15T12:00:00Z");
    jest.setSystemTime(now);

    const lastSeen = new Date("2024-01-15T06:00:00Z").toISOString(); // 6 hours ago
    expect(calculateStatus(lastSeen)).toBe("inactive");
  });

  it("should return 'offline' when last seen more than 24 hours ago", () => {
    const now = new Date("2024-01-15T12:00:00Z");
    jest.setSystemTime(now);

    const lastSeen = new Date("2024-01-13T12:00:00Z").toISOString(); // 48 hours ago
    expect(calculateStatus(lastSeen)).toBe("offline");
  });

  it("should return 'inactive' at exactly 1 hour boundary", () => {
    const now = new Date("2024-01-15T12:00:00Z");
    jest.setSystemTime(now);

    const lastSeen = new Date("2024-01-15T11:00:00Z").toISOString(); // exactly 1 hour ago
    expect(calculateStatus(lastSeen)).toBe("inactive");
  });

  it("should return 'offline' at exactly 24 hour boundary", () => {
    const now = new Date("2024-01-15T12:00:00Z");
    jest.setSystemTime(now);

    const lastSeen = new Date("2024-01-14T12:00:00Z").toISOString(); // exactly 24 hours ago
    expect(calculateStatus(lastSeen)).toBe("offline");
  });
});

describe("DeviceStatusBadge", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date("2024-01-15T12:00:00Z"));
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("should render Active badge for recently seen device", () => {
    const lastSeen = new Date("2024-01-15T11:30:00Z").toISOString();
    render(<DeviceStatusBadge lastSeen={lastSeen} />);

    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("should render Inactive badge for device seen 1-24 hours ago", () => {
    const lastSeen = new Date("2024-01-15T06:00:00Z").toISOString();
    render(<DeviceStatusBadge lastSeen={lastSeen} />);

    expect(screen.getByText("Inactive")).toBeInTheDocument();
  });

  it("should render Offline badge for device seen more than 24 hours ago", () => {
    const lastSeen = new Date("2024-01-13T12:00:00Z").toISOString();
    render(<DeviceStatusBadge lastSeen={lastSeen} />);

    expect(screen.getByText("Offline")).toBeInTheDocument();
  });
});
