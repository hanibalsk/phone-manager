import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { OrganizationFeaturesDialog } from "../organization-features-dialog";
import type { Organization } from "@/types";

// Mock the API client
const mockUpdateFeatures = jest.fn();
jest.mock("@/lib/api-client", () => ({
  organizationsApi: {
    updateFeatures: (...args: unknown[]) => mockUpdateFeatures(...args),
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
    proximity_alerts: false,
    webhooks: true,
    trips: false,
    movement_tracking: true,
  },
  created_at: "2025-01-01T00:00:00Z",
  updated_at: "2025-01-01T00:00:00Z",
};

describe("OrganizationFeaturesDialog", () => {
  const mockOnSuccess = jest.fn();
  const mockOnCancel = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should render with current feature states", () => {
    render(
      <OrganizationFeaturesDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    // Check that feature toggles reflect current state
    const geofencesSwitch = screen.getByRole("switch", { name: /geofences/i });
    const proximityAlertsSwitch = screen.getByRole("switch", { name: /proximity alerts/i });
    const webhooksSwitch = screen.getByRole("switch", { name: /webhooks/i });
    const tripsSwitch = screen.getByRole("switch", { name: /trips/i });
    const movementTrackingSwitch = screen.getByRole("switch", { name: /movement tracking/i });

    expect(geofencesSwitch).toBeChecked();
    expect(proximityAlertsSwitch).not.toBeChecked();
    expect(webhooksSwitch).toBeChecked();
    expect(tripsSwitch).not.toBeChecked();
    expect(movementTrackingSwitch).toBeChecked();
  });

  it("should display organization name in description", () => {
    render(
      <OrganizationFeaturesDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText(/test organization/i)).toBeInTheDocument();
  });

  it("should display feature descriptions", () => {
    render(
      <OrganizationFeaturesDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    expect(screen.getByText(/geofence-based location tracking/i)).toBeInTheDocument();
    expect(screen.getByText(/alerts when devices enter or leave/i)).toBeInTheDocument();
    expect(screen.getByText(/integration with external services/i)).toBeInTheDocument();
  });

  it("should call onCancel when cancel button is clicked", async () => {
    render(
      <OrganizationFeaturesDialog
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
      <OrganizationFeaturesDialog
        organization={mockOrganization}
        onSuccess={mockOnSuccess}
        onCancel={mockOnCancel}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /close dialog/i }));

    expect(mockOnCancel).toHaveBeenCalledTimes(1);
  });

  describe("toggle behavior", () => {
    it("should toggle feature state when clicked", async () => {
      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const proximityAlertsSwitch = screen.getByRole("switch", { name: /proximity alerts/i });
      expect(proximityAlertsSwitch).not.toBeChecked();

      await userEvent.click(proximityAlertsSwitch);

      expect(proximityAlertsSwitch).toBeChecked();
    });

    it("should toggle off a feature that was on", async () => {
      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const geofencesSwitch = screen.getByRole("switch", { name: /geofences/i });
      expect(geofencesSwitch).toBeChecked();

      await userEvent.click(geofencesSwitch);

      expect(geofencesSwitch).not.toBeChecked();
    });
  });

  describe("API interaction", () => {
    it("should not call API when no features are changed", async () => {
      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.click(screen.getByRole("button", { name: /save features/i }));

      expect(mockUpdateFeatures).not.toHaveBeenCalled();
      expect(mockOnSuccess).toHaveBeenCalledTimes(1);
    });

    it("should call API with only changed features", async () => {
      mockUpdateFeatures.mockResolvedValue({ data: mockOrganization });

      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      // Enable proximity_alerts (was false)
      await userEvent.click(screen.getByRole("switch", { name: /proximity alerts/i }));
      await userEvent.click(screen.getByRole("button", { name: /save features/i }));

      await waitFor(() => {
        expect(mockUpdateFeatures).toHaveBeenCalledWith("org-1", { proximity_alerts: true });
      });
      expect(mockOnSuccess).toHaveBeenCalledTimes(1);
    });

    it("should call API with multiple changed features", async () => {
      mockUpdateFeatures.mockResolvedValue({ data: mockOrganization });

      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      // Enable proximity_alerts (was false)
      await userEvent.click(screen.getByRole("switch", { name: /proximity alerts/i }));
      // Disable geofences (was true)
      await userEvent.click(screen.getByRole("switch", { name: /geofences/i }));
      await userEvent.click(screen.getByRole("button", { name: /save features/i }));

      await waitFor(() => {
        expect(mockUpdateFeatures).toHaveBeenCalledWith("org-1", {
          proximity_alerts: true,
          geofences: false,
        });
      });
    });

    it("should display error message on API failure", async () => {
      mockUpdateFeatures.mockResolvedValue({ error: "Failed to update features" });

      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.click(screen.getByRole("switch", { name: /proximity alerts/i }));
      await userEvent.click(screen.getByRole("button", { name: /save features/i }));

      await waitFor(() => {
        expect(screen.getByText(/failed to update features/i)).toBeInTheDocument();
      });
      expect(mockOnSuccess).not.toHaveBeenCalled();
    });

    it("should show saving state while submitting", async () => {
      let resolvePromise: (value: unknown) => void;
      const promise = new Promise((resolve) => {
        resolvePromise = resolve;
      });
      mockUpdateFeatures.mockReturnValue(promise);

      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      await userEvent.click(screen.getByRole("switch", { name: /proximity alerts/i }));
      await userEvent.click(screen.getByRole("button", { name: /save features/i }));

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
        <OrganizationFeaturesDialog
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

    it("should render all feature toggles as switches", () => {
      render(
        <OrganizationFeaturesDialog
          organization={mockOrganization}
          onSuccess={mockOnSuccess}
          onCancel={mockOnCancel}
        />
      );

      const switches = screen.getAllByRole("switch");
      expect(switches).toHaveLength(5); // 5 features
    });
  });
});
