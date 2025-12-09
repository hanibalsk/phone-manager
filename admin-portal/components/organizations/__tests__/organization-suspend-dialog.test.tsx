import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { OrganizationSuspendDialog } from "../organization-suspend-dialog";
import type { Organization } from "@/types";

// Mock the API client
const mockSuspend = jest.fn();
jest.mock("@/lib/api-client", () => ({
  organizationsApi: {
    suspend: (...args: unknown[]) => mockSuspend(...args),
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

describe("OrganizationSuspendDialog", () => {
  const mockOnSuccess = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should render the dialog with organization name", () => {
    render(
      <OrganizationSuspendDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    // Check for title - there's also a button with same text, so get by role
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText(/suspend.*test organization/i)).toBeInTheDocument();
  });

  it("should display warning about user access", () => {
    render(
      <OrganizationSuspendDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText(/warning/i)).toBeInTheDocument();
    expect(screen.getByText(/revoke access for all users/i)).toBeInTheDocument();
  });

  it("should call onCancel when cancel button is clicked", async () => {
    render(
      <OrganizationSuspendDialog
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
      <OrganizationSuspendDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /close dialog/i }));

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  describe("validation", () => {
    it("should show error when reason is empty", async () => {
      render(
        <OrganizationSuspendDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.click(screen.getByRole("button", { name: /suspend organization/i }));

      expect(screen.getByText(/please provide a reason/i)).toBeInTheDocument();
      expect(mockSuspend).not.toHaveBeenCalled();
    });

    it("should show error when reason is only whitespace", async () => {
      render(
        <OrganizationSuspendDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.type(screen.getByLabelText(/reason for suspension/i), "   ");
      await userEvent.click(screen.getByRole("button", { name: /suspend organization/i }));

      expect(screen.getByText(/please provide a reason/i)).toBeInTheDocument();
      expect(mockSuspend).not.toHaveBeenCalled();
    });
  });

  describe("API interaction", () => {
    it("should call API with organization ID and reason", async () => {
      mockSuspend.mockResolvedValue({ data: { ...mockOrganization, status: "suspended" } });

      render(
        <OrganizationSuspendDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.type(
        screen.getByLabelText(/reason for suspension/i),
        "Non-payment of subscription"
      );
      await userEvent.click(screen.getByRole("button", { name: /suspend organization/i }));

      await waitFor(() => {
        expect(mockSuspend).toHaveBeenCalledWith("org-1", "Non-payment of subscription");
      });
      expect(mockOnSuccess).toHaveBeenCalledTimes(1);
    });

    it("should trim the reason before sending", async () => {
      mockSuspend.mockResolvedValue({ data: { ...mockOrganization, status: "suspended" } });

      render(
        <OrganizationSuspendDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.type(
        screen.getByLabelText(/reason for suspension/i),
        "  Policy violation  "
      );
      await userEvent.click(screen.getByRole("button", { name: /suspend organization/i }));

      await waitFor(() => {
        expect(mockSuspend).toHaveBeenCalledWith("org-1", "Policy violation");
      });
    });

    it("should display error message on API failure", async () => {
      mockSuspend.mockResolvedValue({ error: "Failed to suspend organization" });

      render(
        <OrganizationSuspendDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.type(screen.getByLabelText(/reason for suspension/i), "Test reason");
      await userEvent.click(screen.getByRole("button", { name: /suspend organization/i }));

      await waitFor(() => {
        expect(screen.getByText(/failed to suspend organization/i)).toBeInTheDocument();
      });
      expect(mockOnSuccess).not.toHaveBeenCalled();
    });

    it("should show suspending state while submitting", async () => {
      let resolvePromise: (value: unknown) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockSuspend.mockReturnValue(promise);

      render(
        <OrganizationSuspendDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.type(screen.getByLabelText(/reason for suspension/i), "Test reason");
      await userEvent.click(screen.getByRole("button", { name: /suspend organization/i }));

      expect(screen.getByRole("button", { name: /suspending.../i })).toBeDisabled();

      resolvePromise!({ data: { ...mockOrganization, status: "suspended" } });

      await waitFor(() => {
        expect(mockOnSuccess).toHaveBeenCalled();
      });
    });
  });

  describe("accessibility", () => {
    it("should have proper dialog role and aria attributes", () => {
      render(
        <OrganizationSuspendDialog
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

    it("should have required indicator on reason field", () => {
      render(
        <OrganizationSuspendDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      expect(screen.getByText("*")).toBeInTheDocument();
    });
  });
});
