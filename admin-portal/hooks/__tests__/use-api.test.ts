import { renderHook, act, waitFor } from "@testing-library/react";
import { useApi } from "../use-api";
import type { ApiResponse } from "@/types";

describe("useApi", () => {
  describe("initial state", () => {
    it("should have correct initial values", () => {
      const { result } = renderHook(() => useApi());

      expect(result.current.data).toBeNull();
      expect(result.current.error).toBeNull();
      expect(result.current.loading).toBe(false);
    });
  });

  describe("execute", () => {
    it("should set loading to true during execution", async () => {
      const { result } = renderHook(() => useApi<string>());

      let resolvePromise: (value: ApiResponse<string>) => void;
      const fetchFn = jest.fn(
        () =>
          new Promise<ApiResponse<string>>((resolve) => {
            resolvePromise = resolve;
          })
      );

      act(() => {
        result.current.execute(fetchFn);
      });

      expect(result.current.loading).toBe(true);

      await act(async () => {
        resolvePromise!({ data: "test", status: 200 });
      });

      expect(result.current.loading).toBe(false);
    });

    it("should set data on successful response", async () => {
      const { result } = renderHook(() => useApi<string>());
      const mockData = "test data";
      const fetchFn = jest.fn().mockResolvedValue({ data: mockData, status: 200 });

      await act(async () => {
        await result.current.execute(fetchFn);
      });

      expect(result.current.data).toBe(mockData);
      expect(result.current.error).toBeNull();
    });

    it("should set error on error response", async () => {
      const { result } = renderHook(() => useApi<string>());
      const mockError = "Something went wrong";
      const fetchFn = jest.fn().mockResolvedValue({ error: mockError, status: 400 });

      await act(async () => {
        await result.current.execute(fetchFn);
      });

      expect(result.current.data).toBeNull();
      expect(result.current.error).toBe(mockError);
    });

    it("should handle thrown errors", async () => {
      const { result } = renderHook(() => useApi<string>());
      const fetchFn = jest.fn().mockRejectedValue(new Error("Network error"));

      await act(async () => {
        await result.current.execute(fetchFn);
      });

      expect(result.current.error).toBe("Network error");
    });

    it("should call onSuccess callback on success", async () => {
      const onSuccess = jest.fn();
      const { result } = renderHook(() => useApi<string>({ onSuccess }));
      const mockData = "test data";
      const fetchFn = jest.fn().mockResolvedValue({ data: mockData, status: 200 });

      await act(async () => {
        await result.current.execute(fetchFn);
      });

      expect(onSuccess).toHaveBeenCalledWith(mockData);
    });

    it("should call onError callback on error", async () => {
      const onError = jest.fn();
      const { result } = renderHook(() => useApi<string>({ onError }));
      const mockError = "Error message";
      const fetchFn = jest.fn().mockResolvedValue({ error: mockError, status: 400 });

      await act(async () => {
        await result.current.execute(fetchFn);
      });

      expect(onError).toHaveBeenCalledWith(mockError);
    });

    it("should return data on success", async () => {
      const { result } = renderHook(() => useApi<string>());
      const mockData = "test data";
      const fetchFn = jest.fn().mockResolvedValue({ data: mockData, status: 200 });

      let returnedData: string | null = null;
      await act(async () => {
        returnedData = await result.current.execute(fetchFn);
      });

      expect(returnedData).toBe(mockData);
    });

    it("should return null on error", async () => {
      const { result } = renderHook(() => useApi<string>());
      const fetchFn = jest.fn().mockResolvedValue({ error: "error", status: 400 });

      let returnedData: string | null = "should be null";
      await act(async () => {
        returnedData = await result.current.execute(fetchFn);
      });

      expect(returnedData).toBeNull();
    });
  });

  describe("reset", () => {
    it("should reset all state to initial values", async () => {
      const { result } = renderHook(() => useApi<string>());
      const fetchFn = jest.fn().mockResolvedValue({ data: "test", status: 200 });

      await act(async () => {
        await result.current.execute(fetchFn);
      });

      expect(result.current.data).toBe("test");

      act(() => {
        result.current.reset();
      });

      expect(result.current.data).toBeNull();
      expect(result.current.error).toBeNull();
      expect(result.current.loading).toBe(false);
    });
  });
});
