import { useState, useEffect, useCallback } from 'react';
import { healthService } from '../services/healthService';

export function useHealthCheck(autoPoll = false, pollInterval = 10000) {
  const [healthData, setHealthData] = useState(null);
  const [latency, setLatency] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);

  const fetchHealth = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await healthService.checkHealth();
      setHealthData(res.data);
      setLatency(res.latencyMs);
      setLastChecked(new Date());
    } catch (err) {
      setError(err.userFriendlyMessage || 'Backend unreachable');
      setHealthData(null);
      setLatency(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchHealth();

    if (autoPoll) {
      const timer = setInterval(fetchHealth, pollInterval);
      return () => clearInterval(timer);
    }
  }, [fetchHealth, autoPoll, pollInterval]);

  return {
    healthData,
    latency,
    isLoading,
    error,
    lastChecked,
    refresh: fetchHealth,
    isOnline: !!healthData && !error
  };
}
