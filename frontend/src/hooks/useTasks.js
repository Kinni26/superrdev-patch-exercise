import { useState, useEffect } from 'react';
import { fetchTasks } from '../api';

export function useTasks(query, status, page, pageSize) {
  const [tasks, setTasks] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Abort the in-flight request when inputs change or the component unmounts,
    // so a slow, older response can never overwrite newer results.
    const controller = new AbortController();
    setLoading(true);
    setError(null);

    fetchTasks({ query, status, page, pageSize }, { signal: controller.signal })
      .then((data) => {
        setTasks(data.items);
        setTotal(data.total);
      })
      .catch((err) => {
        if (err.name === 'AbortError') return;
        setTasks([]);
        setTotal(0);
        setError(err.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });

    return () => controller.abort();
  }, [query, status, page, pageSize]);

  return { tasks, total, loading, error };
}
