/**
 * Helpers for resolving and fetching JWT-protected backend files (/api/files/{storedFileName}).
 * Keeps bearer tokens strictly restricted to the configured API origin and prevents
 * plain <img> or <a> tags from failing with 401/403 or hitting the frontend origin.
 */

const PROTECTED_FILES_SEGMENT = '/api/files/';

/**
 * Extracts the stored filename if `url` points to the backend's protected `/api/files/{name}` endpoint.
 * Returns null for empty values, local blob:/data: URLs, or external third-party URLs (e.g. Unsplash).
 */
export function extractProtectedFileName(url?: string | null): string | null {
  if (!url) return null;
  const trimmed = url.trim();
  if (!trimmed || trimmed.startsWith('blob:') || trimmed.startsWith('data:')) {
    return null;
  }

  let pathname = trimmed;
  if (trimmed.startsWith('http://') || trimmed.startsWith('https://')) {
    try {
      pathname = new URL(trimmed).pathname;
    } catch {
      return null;
    }
  }

  const markerIndex = pathname.indexOf(PROTECTED_FILES_SEGMENT);
  if (markerIndex === -1) {
    return null;
  }

  const fileName = pathname.slice(markerIndex + PROTECTED_FILES_SEGMENT.length);
  if (!fileName || fileName.includes('..') || fileName.includes('/') || fileName.includes('\\')) {
    return null;
  }

  return fileName;
}

/**
 * Resolves a protected file reference against the configured backend API base URL.
 */
export function resolveProtectedFileEndpoint(url: string, apiBaseUrl: string): string {
  const fileName = extractProtectedFileName(url);
  if (!fileName) {
    throw new Error('Invalid protected file URL');
  }
  const normalizedBase = apiBaseUrl.replace(/\/+$/, '');
  return `${normalizedBase}/files/${encodeURIComponent(fileName)}`;
}

/**
 * Fetches a protected backend file using JWT Bearer authentication and returns its Blob.
 */
export async function fetchProtectedFileBlob(
  url: string,
  apiBaseUrl: string,
  token: string | null,
  signal?: AbortSignal
): Promise<Blob> {
  if (!token) {
    throw new Error('Authentication required. Please sign in again.');
  }

  const endpoint = resolveProtectedFileEndpoint(url, apiBaseUrl);
  const response = await fetch(endpoint, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
    },
    signal,
  });

  if (response.status === 401) {
    throw new Error('Your session has expired. Please sign in again.');
  }
  if (response.status === 403) {
    throw new Error('You do not have permission to view this file.');
  }
  if (response.status === 404) {
    throw new Error('File not found.');
  }
  if (!response.ok) {
    throw new Error(`Failed to load file (HTTP ${response.status}).`);
  }

  return response.blob();
}

