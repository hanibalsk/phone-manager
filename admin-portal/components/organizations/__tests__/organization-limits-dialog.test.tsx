import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { OrganizationLimitsDialog } from "../organization-limits-dialog";
import type { Organization } from "@/types";

// Mock the API client
const mockUpdateLimits = jest.fn();
jest.mock("@/lib/api-client", () => ({
  organizationsApi: {
    updateLimits: (...args: unknown[]) => mockUpdateLimits(...args),
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

describe("OrganizationLimitsDialog", () => {
  const mockOnSuccess = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should render with current limit values", () => {
    render(
      <OrganizationLimitsDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByLabelText(/maximum users/i)).toHaveValue(100);
    expect(screen.getByLabelText(/maximum devices/i)).toHaveValue(500);
    expect(screen.getByLabelText(/maximum groups/i)).toHaveValue(50);
  });

  it("should display organization name in description", () => {
    render(
      <OrganizationLimitsDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText(/test organization/i)).toBeInTheDocument();
  });

  it("should call onCancel when cancel button is clicked", async () => {
    render(
      <OrganizationLimitsDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /cancel/i }));

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  it("should call onCancel when close button is clicked", async () => {
    render(
      <OrganizationLimitsDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /close dialog/i }));

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  describe("validation", () => {
    it("should allow 0 for max_groups (unlimited)", async () => {
      mockUpdateLimits.mockResolvedValue({ data: mockOrganization });

      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const maxGroupsInput = screen.getByLabelText(/maximum groups/i);
      await userEvent.clear(maxGroupsInput);
      await userEvent.type(maxGroupsInput, "0");
      await userEvent.click(screen.getByRole("button", { name: /save limits/i }));

      await waitFor(() => {
        expect(mockUpdateLimits).toHaveBeenCalled();
      });
    });
  });

  describe("API interaction", () => {
    it("should not call API when values are unchanged", async () => {
      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.click(screen.getByRole("button", { name: /save limits/i }));

      expect(mockUpdateLimits).not.toHaveBeenCalled();
      expect(mockOnSuccess).toHaveBeenCalledTimes(1);
    });

    it("should call API with only changed values", async () => {
      mockUpdateLimits.mockResolvedValue({ data: mockOrganization });

      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const maxUsersInput = screen.getByLabelText(/maximum users/i);
      await userEvent.clear(maxUsersInput);
      await userEvent.type(maxUsersInput, "200");
      await userEvent.click(screen.getByRole("button", { name: /save limits/i }));

      await waitFor(() => {
        expect(mockUpdateLimits).toHaveBeenCalledWith("org-1", { max_users: 200 });
      });
      expect(mockOnSuccess).toHaveBeenCalledTimes(1);
    });

    it("should call API with multiple changed values", async () => {
      mockUpdateLimits.mockResolvedValue({ data: mockOrganization });

      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const maxUsersInput = screen.getByLabelText(/maximum users/i);
      const maxDevicesInput = screen.getByLabelText(/maximum devices/i);

      await userEvent.clear(maxUsersInput);
      await userEvent.type(maxUsersInput, "150");
      await userEvent.clear(maxDevicesInput);
      await userEvent.type(maxDevicesInput, "1000");
      await userEvent.click(screen.getByRole("button", { name: /save limits/i }));

      await waitFor(() => {
        expect(mockUpdateLimits).toHaveBeenCalledWith("org-1", {
          max_users: 150,
          max_devices: 1000,
        });
      });
    });

    it("should display error message on API failure", async () => {
      mockUpdateLimits.mockResolvedValue({ error: "Failed to update limits" });

      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const maxUsersInput = screen.getByLabelText(/maximum users/i);
      await userEvent.clear(maxUsersInput);
      await userEvent.type(maxUsersInput, "200");
      await userEvent.click(screen.getByRole("button", { name: /save limits/i }));

      await waitFor(() => {
        expect(screen.getByText(/failed to update limits/i)).toBeInTheDocument();
      });
      expect(mockOnSuccess).not.toHaveBeenCalled();
    });

    it("should show saving state while submitting", async () => {
      let resolvePromise: (value: unknown) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockUpdateLimits.mockReturnValue(promise);

      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const maxUsersInput = screen.getByLabelText(/maximum users/i);
      await userEvent.clear(maxUsersInput);
      await userEvent.type(maxUsersInput, "200");
      await userEvent.click(screen.getByRole("button", { name: /save limits/i }));

      expect(screen.getByRole("button", { name: /saving.../i })).toBeDisabled();

      resolvePromise!({ data: mockOrganization });

      await waitFor(() => {
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });
  });

  describe("accessibility", () => {
    it("should have proper dialog role and aria attributes", () => {
      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const dialog = screen.getByRole("dialog");
      expect(dialog).toHaveAttribute("aria-modal", "true");
      expect(dialog).toHaveAttribute("aria-labelledby");
      expect(dialog).toHaveAttribute("aria-describedby");
    });

    it("should have descriptive labels for all inputs", () => {
      render(
        <OrganizationLimitsDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByLabelText(/maximum users/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/maximum devices/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/maximum groups/i)).toBeInTheDocument();
    });
  });
});
