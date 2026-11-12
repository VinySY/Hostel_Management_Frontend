import type { AuthResponse, UserRole } from '../types';

export const DEMO_READ_ONLY_NOTICE =
  'Read-Only Demo Mode: You are exploring an isolated showcase hostel. Creating, editing, and deleting records is disabled.';

export function isPublicDemoLoginEnabled(envValue: string | boolean | undefined): boolean {
  return envValue === true || envValue === 'true';
}

export function extractErrorMessageFromBody(
  body: unknown,
  status: number,
  fallbackMessage: string
): string {
  if (body && typeof body === 'object' && 'message' in body) {
    const raw = (body as { message?: unknown }).message;
    if (typeof raw === 'string' && raw.trim() !== '') {
      return raw.trim();
    }
  }
  if (status === 403) {
    return 'Demo accounts are read-only. Modifying, creating, or deleting records is disabled in Demo Mode.';
  }
  if (status >= 500) {
    return 'The server could not process the request. Please try again.';
  }
  return fallbackMessage;
}

export async function throwIfApiError(res: Response, fallbackMessage: string): Promise<void> {
  if (res.ok) return;
  const body = await res.json().catch(() => null);
  throw new Error(extractErrorMessageFromBody(body, res.status, fallbackMessage));
}

export async function submitDemoLoginRequest(
  fetchFn: typeof fetch,
  apiBaseUrl: string,
  role: UserRole
): Promise<AuthResponse> {
  let response: Response;
  try {
    response = await fetchFn(`${apiBaseUrl}/auth/demo-login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ role }),
    });
  } catch {
    throw new Error(
      `Unable to reach the authentication server at ${apiBaseUrl}. Please verify the backend is running.`
    );
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null);
    if (response.status === 403) {
      throw new Error(
        extractErrorMessageFromBody(
          body,
          403,
          'Public Demo Mode is currently disabled on this server.'
        )
      );
    }
    throw new Error(
      extractErrorMessageFromBody(body, response.status, `Demo login failed (HTTP ${response.status}).`)
    );
  }

  return response.json();
}

