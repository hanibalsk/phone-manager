import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { OrganizationStatsDialog } from "../organization-stats-dialog";
import type { Organization } from "@/types";
import type { OrganizationStats } from "@/lib/api-client";

// Mock the API client
const mockGetStats = jest.fn();
jest.mock("@/lib/api-client", () => ({
  organizationsApi: {
    getStats: (...args: unknown[]) => mockGetStats(...args),
  },
}));

// Mock the focus trap hook
jest.mock("@/hooks/use-focus-trap", () => ({
  useFocusTrap: () => ({ current: null }),
}));

const mockOrganization: Organization = {
  id: "org-1",
  name: "Test Organization",
  slug: "test-org",
  type: "enterprise",
  status: "active",
  contact_email: "test@example.com",
  max_users: 100,
  max_devices: 500,
  max_groups: 50,
  features: {
    geofences: true,
    proximity_alerts: true,
    webhooks: true,
    trips: true,
    movement_tracking: true,
  },
  created_at: "2025-01-01T00:00:00Z",
  updated_at: "2025-01-01T00:00:00Z",
};

const mockStats: OrganizationStats = {
  users_count: 45,
  devices_count: 230,
  groups_count: 12,
  storage_used_mb: 256,
  usage_trends: [
    { period: "2025-01", users: 40, devices: 200 },
    { period: "2025-02", users: 42, devices: 215 },
    { period: "2025-03", users: 45, devices: 230 },
  ],
};

describe("OrganizationStatsDialog", () => {
  const mockOnClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("loading state", () => {
    it("should show loading spinner on initial render", async () => {
      let resolvePromise: (value: unknown) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockGetStats.mockReturnValue(promise);

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      expect(screen.getByRole("dialog")).toBeInTheDocument();
      // Stats content should not be visible during loading
      expect(screen.queryByText(/of 100 users/i)).not.toBeInTheDocument();

      // Cleanup: resolve the promise
      resolvePromise!({ data: mockStats });
      await waitFor(() => {
        expect(screen.getByText(/of 100 users/i)).toBeInTheDocument();
      });
    });
  });

  describe("data display", () => {
    it("should display organization name in description", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/test organization/i)).toBeInTheDocument();
      });
    });

    it("should display users count with limit", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/of 100 users/i)).toBeInTheDocument();
      });
    });

    it("should display devices count with limit", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/of 500 devices/i)).toBeInTheDocument();
      });
    });

    it("should display groups count with limit", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/of 50 groups/i)).toBeInTheDocument();
      });
    });

    it("should display infinity symbol when max_groups is not set", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });
      const orgWithUnlimitedGroups = { ...mockOrganization, max_groups: 0 };

      render(
        <OrganizationStatsDialog
          organization={orgWithUnlimitedGroups}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/of ∞ groups/i)).toBeInTheDocument();
      });
    });
  });

  describe("storage formatting", () => {
    it("should display storage in MB when under 1024 MB", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText("256 MB")).toBeInTheDocument();
      });
    });

    it("should display storage in GB when 1024 MB or more", async () => {
      const statsWithLargeStorage: OrganizationStats = {
        ...mockStats,
        storage_used_mb: 2560,
      };
      mockGetStats.mockResolvedValue({ data: statsWithLargeStorage });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText("2.5 GB")).toBeInTheDocument();
      });
    });

    it("should display storage in GB with one decimal place", async () => {
      const statsWithLargeStorage: OrganizationStats = {
        ...mockStats,
        storage_used_mb: 1536, // 1.5 GB
      };
      mockGetStats.mockResolvedValue({ data: statsWithLargeStorage });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText("1.5 GB")).toBeInTheDocument();
      });
    });
  });

  describe("usage trends", () => {
    it("should display usage trends table", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText("Usage Trends")).toBeInTheDocument();
        expect(screen.getByRole("table")).toBeInTheDocument();
      });
    });

    it("should display trend periods and data", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText("2025-01")).toBeInTheDocument();
        expect(screen.getByText("2025-02")).toBeInTheDocument();
        expect(screen.getByText("2025-03")).toBeInTheDocument();
      });
    });

    it("should not display usage trends section when empty", async () => {
      const statsWithoutTrends: OrganizationStats = {
        ...mockStats,
        usage_trends: [],
      };
      mockGetStats.mockResolvedValue({ data: statsWithoutTrends });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText("45")).toBeInTheDocument(); // Stats loaded
      });

      expect(screen.queryByText("Usage Trends")).not.toBeInTheDocument();
    });
  });

  describe("error handling", () => {
    it("should display error message on API failure", async () => {
      mockGetStats.mockResolvedValue({ error: "Failed to load statistics" });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/failed to load statistics/i)).toBeInTheDocument();
      });
    });

    it("should display retry button on error", async () => {
      mockGetStats.mockResolvedValue({ error: "Failed to load statistics" });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByRole("button", { name: /retry/i })).toBeInTheDocument();
      });
    });

    it("should retry fetching stats when retry button is clicked", async () => {
      mockGetStats
        .mockResolvedValueOnce({ error: "Failed to load statistics" })
        .mockResolvedValueOnce({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/failed to load statistics/i)).toBeInTheDocument();
      });

      await userEvent.click(screen.getByRole("button", { name: /retry/i }));

      await waitFor(() => {
        expect(screen.getByText(/of 100 users/i)).toBeInTheDocument();
      });
      expect(mockGetStats).toHaveBeenCalledTimes(2);
    });

    it("should handle exception errors", async () => {
      mockGetStats.mockRejectedValue(new Error("Network error"));

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText("Network error")).toBeInTheDocument();
      });
    });
  });

  describe("refresh functionality", () => {
    it("should display refresh button when stats are loaded", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/of 100 users/i)).toBeInTheDocument();
      });

      // The main refresh button at the bottom
      const refreshButtons = screen.getAllByRole("button", { name: /refresh/i });
      expect(refreshButtons.length).toBeGreaterThanOrEqual(1);
    });

    it("should refresh stats when refresh button is clicked", async () => {
      const updatedStats: OrganizationStats = {
        ...mockStats,
        users_count: 50,
      };
      mockGetStats
        .mockResolvedValueOnce({ data: mockStats })
        .mockResolvedValueOnce({ data: updatedStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/of 100 users/i)).toBeInTheDocument();
      });

      // Click the refresh button
      const refreshButtons = screen.getAllByRole("button", { name: /refresh/i });
      await userEvent.click(refreshButtons[0]);

      await waitFor(() => {
        expect(screen.getByText("50")).toBeInTheDocument();
      });
      expect(mockGetStats).toHaveBeenCalledTimes(2);
    });
  });

  describe("close functionality", () => {
    it("should call onClose when close button is clicked", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(screen.getByText(/of 100 users/i)).toBeInTheDocument();
      });

      await userEvent.click(screen.getByRole("button", { name: /close dialog/i }));

      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });
  });

  describe("accessibility", () => {
    it("should have proper dialog role and aria attributes", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      const dialog = screen.getByRole("dialog");
      expect(dialog).toHaveAttribute("aria-modal", "true");
      expect(dialog).toHaveAttribute("aria-labelledby");
      expect(dialog).toHaveAttribute("aria-describedby");
    });

    it("should have organization statistics title", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      expect(screen.getByText("Organization Statistics")).toBeInTheDocument();
    });
  });

  describe("API calls", () => {
    it("should call getStats with organization ID on mount", async () => {
      mockGetStats.mockResolvedValue({ data: mockStats });

      render(
        <OrganizationStatsDialog
          organization={mockOrganization}
          onClose={mockOnClose}
        />
      );

      await waitFor(() => {
        expect(mockGetStats).toHaveBeenCalledWith("org-1");
      });
    });
  });
});
