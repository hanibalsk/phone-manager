import { render, screen } from "@testing-library/react";
import { PermissionGuard, withPermission } from "../permission-guard";

// Mock the usePermissions hook
const mockUsePermissions = jest.fn();
jest.mock("@/hooks/use-permissions", () => ({
  usePermissions: () => mockUsePermissions(),
}));

describe("PermissionGuard", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("with single permission", () => {
    it("should render children when user has the permission", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn().mockReturnValue(true),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard permission="users.read">
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.getByTestId("protected-content")).toBeInTheDocument();
    });

    it("should render nothing when user lacks the permission", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn().mockReturnValue(false),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard permission="users.read">
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.queryByTestId("protected-content")).not.toBeInTheDocument();
    });
  });

  describe("with multiple permissions (requireAll=false, default)", () => {
    it("should render children when user has any of the permissions", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn(),
        hasAnyPermission: jest.fn().mockReturnValue(true),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard permission={["users.read", "users.create"]}>
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.getByTestId("protected-content")).toBeInTheDocument();
    });

    it("should render nothing when user has none of the permissions", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn(),
        hasAnyPermission: jest.fn().mockReturnValue(false),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard permission={["users.read", "users.create"]}>
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.queryByTestId("protected-content")).not.toBeInTheDocument();
    });
  });

  describe("with multiple permissions (requireAll=true)", () => {
    it("should render children when user has all permissions", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn(),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn().mockReturnValue(true),
      });

      render(
        <PermissionGuard permission={["users.read", "users.create"]} requireAll>
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.getByTestId("protected-content")).toBeInTheDocument();
    });

    it("should render nothing when user lacks any permission", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn(),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn().mockReturnValue(false),
      });

      render(
        <PermissionGuard permission={["users.read", "users.create"]} requireAll>
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.queryByTestId("protected-content")).not.toBeInTheDocument();
    });
  });

  describe("with fallback content", () => {
    it("should render fallback when user lacks permission", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn().mockReturnValue(false),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard
          permission="users.read"
          fallback={<div data-testid="fallback">Access Denied</div>}
        >
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.queryByTestId("protected-content")).not.toBeInTheDocument();
      expect(screen.getByTestId("fallback")).toBeInTheDocument();
    });

    it("should not render fallback when user has permission", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn().mockReturnValue(true),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard
          permission="users.read"
          fallback={<div data-testid="fallback">Access Denied</div>}
        >
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.getByTestId("protected-content")).toBeInTheDocument();
      expect(screen.queryByTestId("fallback")).not.toBeInTheDocument();
    });
  });

  describe("with showDenied prop", () => {
    it("should render denied message when user lacks permission and showDenied is true", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn().mockReturnValue(false),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard permission="users.read" showDenied>
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.queryByTestId("protected-content")).not.toBeInTheDocument();
      expect(screen.getByText("Access Denied")).toBeInTheDocument();
    });

    it("should prefer fallback over showDenied", () => {
      mockUsePermissions.mockReturnValue({
        hasPermission: jest.fn().mockReturnValue(false),
        hasAnyPermission: jest.fn(),
        hasAllPermissions: jest.fn(),
      });

      render(
        <PermissionGuard
          permission="users.read"
          showDenied
          fallback={<div data-testid="custom-fallback">Custom Fallback</div>}
        >
          <div data-testid="protected-content">Protected Content</div>
        </PermissionGuard>
      );

      expect(screen.getByTestId("custom-fallback")).toBeInTheDocument();
      expect(screen.queryByText("Access Denied")).not.toBeInTheDocument();
    });
  });
});

describe("withPermission HOC", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("should wrap component with PermissionGuard", () => {
    mockUsePermissions.mockReturnValue({
      hasPermission: jest.fn().mockReturnValue(true),
      hasAnyPermission: jest.fn(),
      hasAllPermissions: jest.fn(),
    });

    const TestComponent = ({ message }: { message: string }) => (
      <div data-testid="test-component">{message}</div>
    );

    const ProtectedComponent = withPermission(TestComponent, "users.read");

    render(<ProtectedComponent message="Hello" />);

    expect(screen.getByTestId("test-component")).toBeInTheDocument();
    expect(screen.getByText("Hello")).toBeInTheDocument();
  });

  it("should hide component when user lacks permission", () => {
    mockUsePermissions.mockReturnValue({
      hasPermission: jest.fn().mockReturnValue(false),
      hasAnyPermission: jest.fn(),
      hasAllPermissions: jest.fn(),
    });

    const TestComponent = ({ message }: { message: string }) => (
      <div data-testid="test-component">{message}</div>
    );

    const ProtectedComponent = withPermission(TestComponent, "users.read");

    render(<ProtectedComponent message="Hello" />);

    expect(screen.queryByTestId("test-component")).not.toBeInTheDocument();
  });

  it("should support multiple permissions with requireAll option", () => {
    mockUsePermissions.mockReturnValue({
      hasPermission: jest.fn(),
      hasAnyPermission: jest.fn(),
      hasAllPermissions: jest.fn().mockReturnValue(true),
    });

    const TestComponent = () => <div data-testid="test-component">Content</div>;

    const ProtectedComponent = withPermission(
      TestComponent,
      ["users.read", "users.create"],
      { requireAll: true }
    );

    render(<ProtectedComponent />);

    expect(screen.getByTestId("test-component")).toBeInTheDocument();
  });

  it("should support fallback option", () => {
    mockUsePermissions.mockReturnValue({
      hasPermission: jest.fn().mockReturnValue(false),
      hasAnyPermission: jest.fn(),
      hasAllPermissions: jest.fn(),
    });

    const TestComponent = () => <div data-testid="test-component">Content</div>;
    const FallbackComponent = <div data-testid="fallback">No Access</div>;

    const ProtectedComponent = withPermission(TestComponent, "users.read", {
      fallback: FallbackComponent,
    });

    render(<ProtectedComponent />);

    expect(screen.queryByTestId("test-component")).not.toBeInTheDocument();
    expect(screen.getByTestId("fallback")).toBeInTheDocument();
  });

  it("should set correct displayName", () => {
    const TestComponent = () => <div>Content</div>;
    TestComponent.displayName = "TestComponent";

    const ProtectedComponent = withPermission(TestComponent, "users.read");

    expect(ProtectedComponent.displayName).toBe("WithPermission(TestComponent)");
  });
});
