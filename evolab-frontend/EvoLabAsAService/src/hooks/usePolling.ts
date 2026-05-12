import { useCallback, useEffect, useRef, useState } from 'react';

export function usePolling<T>(
  fetchFn: () => Promise<T>,
  intervalMs: number,
  shouldStop?: (data: T) => boolean
): { data: T | null; isLoading: boolean; error: string | null; refresh: () => void } {
  const [data, setData] = useState<T | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const lastDataRef = useRef<string | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const stoppedRef = useRef(false);
  const fetchFnRef = useRef(fetchFn);
  const shouldStopRef = useRef(shouldStop);

  useEffect(() => { fetchFnRef.current = fetchFn; });
  useEffect(() => { shouldStopRef.current = shouldStop; });

  const stopPolling = useCallback(() => {
    stoppedRef.current = true;
    if (intervalRef.current !== null) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  const poll = useCallback(async (isInitial: boolean) => {
    try {
      const result = await fetchFnRef.current();
      const serialized = JSON.stringify(result);
      if (serialized !== lastDataRef.current) {
        lastDataRef.current = serialized;
        setData(result);
      }
      setError(null);
      if (shouldStopRef.current?.(result)) {
        stopPolling();
      }
    } catch (err) {
      if (isInitial) {
        setError(err instanceof Error ? err.message : 'Failed to load data.');
      } else {
        console.warn('[usePolling] poll error:', err);
        setError(err instanceof Error ? err.message : 'Failed to refresh data.');
      }
    } finally {
      if (isInitial) setIsLoading(false);
    }
  }, [stopPolling]);

  const start = useCallback((silent = false) => {
    stoppedRef.current = false;
    if (!silent) {
      lastDataRef.current = null;
      setIsLoading(true);
      setError(null);
    }
    poll(!silent).then(() => {
      if (!stoppedRef.current) {
        intervalRef.current = setInterval(() => {
          if (!stoppedRef.current) poll(false);
        }, intervalMs);
      }
    });
  }, [poll, intervalMs]);

  const refresh = useCallback(() => {
    stopPolling();
    start(true);
  }, [stopPolling, start]);

  useEffect(() => {
    start(false);
    return stopPolling;
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return { data, isLoading, error, refresh };
}
