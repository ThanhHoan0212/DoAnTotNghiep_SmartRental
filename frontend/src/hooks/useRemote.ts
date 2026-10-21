import { useEffect, useState } from 'react';
import { api } from '../services/api';
import { message } from '../services/adminFormat';

export function useRemote<T>(url: string, revision = 0) {
  const key = `${url}:${revision}`;
  const [result, setResult] = useState<{ key: string; data: T | null; error: string }>({ key: '', data: null, error: '' });
  useEffect(() => {
    let active = true;
    api.get<T>(url).then(res => { if (active) setResult({ key, data: res.data, error: '' }); })
      .catch(err => { if (active) setResult({ key, data: null, error: message(err) }); });
    return () => { active = false; };
  }, [url, key]);
  return result.key === key
    ? { data: result.data, error: result.error, loading: false }
    : { data: null, error: '', loading: true };
}
