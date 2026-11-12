import { useEffect, useState } from 'react';
import { apiService } from '../services/api';
import { extractProtectedFileName } from '../utils/fileAccess';

export interface ProtectedFileState {
  objectUrl: string | null;
  isLoading: boolean;
  error: string | null;
}

/**
 * Fetches a JWT-protected `/api/files/{name}` resource into an in-memory Blob URL
 * and revokes it automatically on unmount or source change to prevent memory leaks.
 * External URLs and local `blob:`/`data:` URLs pass through without extra network calls.
 */
export function useProtectedFileUrl(src?: string | null): ProtectedFileState {
  const [state, setState] = useState<ProtectedFileState>(() => {
    if (!src) {
      return { objectUrl: null, isLoading: false, error: null };
    }
    if (!extractProtectedFileName(src)) {
      return { objectUrl: src, isLoading: false, error: null };
    }
    return { objectUrl: null, isLoading: true, error: null };
  });

  useEffect(() => {
    if (!src) {
      setState({ objectUrl: null, isLoading: false, error: null });
      return;
    }

    if (!extractProtectedFileName(src)) {
      setState({ objectUrl: src, isLoading: false, error: null });
      return;
    }

    const controller = new AbortController();
    let createdBlobUrl: string | null = null;
    let cancelled = false;

    setState({ objectUrl: null, isLoading: true, error: null });

    apiService
      .fetchProtectedFile(src, controller.signal)
      .then((blob) => {
        if (cancelled) return;
        createdBlobUrl = URL.createObjectURL(blob);
        setState({ objectUrl: createdBlobUrl, isLoading: false, error: null });
      })
      .catch((err: unknown) => {
        if (cancelled || (err instanceof DOMException && err.name === 'AbortError')) {
          return;
        }
        const message = err instanceof Error ? err.message : 'Unable to load file.';
        setState({ objectUrl: null, isLoading: false, error: message });
      });

    return () => {
      cancelled = true;
      controller.abort();
      if (createdBlobUrl) {
        URL.revokeObjectURL(createdBlobUrl);
      }
    };
  }, [src]);

  return state;
}

