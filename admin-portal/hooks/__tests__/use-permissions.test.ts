import { renderHook } from "@testing-library/react";
import { usePermissions } from "../use-permissions";

// Mock the auth context
const mockUseAuth = jest.fn();
jest.mock("@/contexts/auth-context", () => ({
  useAuth: () => mockUseAuth(),
}));

describe("usePermissions", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe("when user is not authenticated", () => {
    beforeEach(() => {
      mockUseAuth.mockReturnValue({
        user: null,
        isAuthenticated: false,
      });
    });

    it("should return false for hasPermission", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasPermission("users.read")).toBe(false);
    });

    it("should return false for hasAnyPermission", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAnyPermission(["users.read", "users.create"])).toBe(false);
    });

    it("should return false for hasAllPermissions", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAllPermissions(["users.read"])).toBe(false);
    });

    it("should return false for hasRole", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasRole("admin")).toBe(false);
    });

    it("should return false for isSuperAdmin", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.isSuperAdmin).toBe(false);
    });

    it("should return empty arrays for permissions and roles", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.permissions).toEqual([]);
      expect(result.current.roles).toEqual([]);
    });
  });

  describe("when user is a regular user", () => {
    beforeEach(() => {
      mockUseAuth.mockReturnValue({
        user: {
          id: "1",
          email: "user@test.com",
          permissions: ["users.read", "devices.read", "LOCATIONS.READ"],
          roles: [
            { role_code: "viewer", role_name: "Viewer", organization_id: "org1" },
          ],
        },
        isAuthenticated: true,
      });
    });

    it("should return true for hasPermission when user has the permission", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasPermission("users.read")).toBe(true);
    });

    it("should return true for hasPermission with case-insensitive matching", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasPermission("USERS.READ")).toBe(true);
      expect(result.current.hasPermission("locations.read")).toBe(true);
    });

    it("should return false for hasPermission when user lacks the permission", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasPermission("users.create")).toBe(false);
    });

    it("should return true for hasAnyPermission when user has at least one", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAnyPermission(["users.read", "users.create"])).toBe(true);
    });

    it("should return false for hasAnyPermission when user has none", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAnyPermission(["users.create", "users.delete"])).toBe(false);
    });

    it("should return true for hasAllPermissions when user has all", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAllPermissions(["users.read", "devices.read"])).toBe(true);
    });

    it("should return false for hasAllPermissions when user lacks any", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAllPermissions(["users.read", "users.create"])).toBe(false);
    });

    it("should return true for hasRole when user has the role", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasRole("viewer")).toBe(true);
    });

    it("should return false for hasRole when user lacks the role", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasRole("admin")).toBe(false);
    });

    it("should return false for isSuperAdmin", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.isSuperAdmin).toBe(false);
    });

    it("should return false for isOrgAdmin", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.isOrgAdmin).toBe(false);
    });
  });

  describe("when user is a super admin", () => {
    beforeEach(() => {
      mockUseAuth.mockReturnValue({
        user: {
          id: "1",
          email: "admin@test.com",
          permissions: [], // Super admin doesn't need explicit permissions
          roles: [
            { role_code: "super_admin", role_name: "Super Admin", organization_id: null },
          ],
        },
        isAuthenticated: true,
      });
    });

    it("should return true for hasPermission even without explicit permission", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasPermission("users.delete")).toBe(true);
      expect(result.current.hasPermission("any.permission")).toBe(true);
    });

    it("should return true for hasAnyPermission even without explicit permissions", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAnyPermission(["users.create", "users.delete"])).toBe(true);
    });

    it("should return true for hasAllPermissions even without explicit permissions", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasAllPermissions(["users.create", "users.delete", "config.update"])).toBe(true);
    });

    it("should return true for isSuperAdmin", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.isSuperAdmin).toBe(true);
    });

    it("should return true for isOrgAdmin", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.isOrgAdmin).toBe(true);
    });
  });

  describe("when user is an org admin", () => {
    beforeEach(() => {
      mockUseAuth.mockReturnValue({
        user: {
          id: "1",
          email: "orgadmin@test.com",
          permissions: ["users.read", "users.create"],
          roles: [
            { role_code: "org_admin", role_name: "Organization Admin", organization_id: "org1" },
          ],
        },
        isAuthenticated: true,
      });
    });

    it("should return false for isSuperAdmin", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.isSuperAdmin).toBe(false);
    });

    it("should return true for isOrgAdmin", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.isOrgAdmin).toBe(true);
    });

    it("should still require explicit permissions (not super admin bypass)", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasPermission("users.read")).toBe(true);
      expect(result.current.hasPermission("users.delete")).toBe(false);
    });
  });

  describe("when user has multiple roles", () => {
    beforeEach(() => {
      mockUseAuth.mockReturnValue({
        user: {
          id: "1",
          email: "multi@test.com",
          permissions: ["users.read"],
          roles: [
            { role_code: "viewer", role_name: "Viewer", organization_id: "org1" },
            { role_code: "support", role_name: "Support", organization_id: "org2" },
          ],
        },
        isAuthenticated: true,
      });
    });

    it("should return true for hasRole for any role the user has", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.hasRole("viewer")).toBe(true);
      expect(result.current.hasRole("support")).toBe(true);
    });

    it("should return all roles", () => {
      const { result } = renderHook(() => usePermissions());
      expect(result.current.roles).toHaveLength(2);
    });
  });
});
