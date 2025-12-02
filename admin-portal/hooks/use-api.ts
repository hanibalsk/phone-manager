"use client";

import { useState, useCallback } from "react";
import type { ApiResponse } from "@/types";

interface UseApiOptions {
  onSuccess?: <T>(data: T) => void;
  onError?: (error: string) => void;
}

export function useApi<T>(options: UseApiOptions = {}) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const execute = useCallback(
    async (fetchFn: () => Promise<ApiResponse<T>>) => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchFn();

        if (response.error) {
          setError(response.error);
          options.onError?.(response.error);
          return null;
        }

        if (response.data) {
          setData(response.data);
          options.onSuccess?.(response.data);
          return response.data;
        }

        return null;
      } catch (err) {
        const errorMessage =
          err instanceof Error ? err.message : "An error occurred";
        setError(errorMessage);
        options.onError?.(errorMessage);
        return null;
      } finally {
        setLoading(false);
      }
    },
    [options]
  );

  const reset = useCallback(() => {
    setData(null);
    setError(null);
    setLoading(false);
  }, []);

  return {
    data,
    error,
    loading,
    execute,
    reset,
  };
}
