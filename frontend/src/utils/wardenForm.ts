export const WARDEN_PASSWORD_MIN_LENGTH = 6;
export const WARDEN_PASSWORD_MAX_LENGTH = 100;

export interface CreateWardenFormInput {
  employeeId: string;
  name: string;
  email: string;
  phone: string;
  hostelId?: number;
  password: string;
  assignedFloors?: number[];
}

/**
 * Mirrors backend WardenCreateRequest password constraints without echoing sensitive values.
 */
export function validateWardenCreatePassword(password: string): string | null {
  if (!password || !password.trim()) {
    return 'Password is required';
  }
  if (password.length < WARDEN_PASSWORD_MIN_LENGTH || password.length > WARDEN_PASSWORD_MAX_LENGTH) {
    return `Password must be between ${WARDEN_PASSWORD_MIN_LENGTH} and ${WARDEN_PASSWORD_MAX_LENGTH} characters`;
  }
  return null;
}

/**
 * Builds the exact JSON payload expected by POST /api/dgm/wardens (WardenCreateRequest).
 */
export function buildCreateWardenPayload(input: CreateWardenFormInput) {
  return {
    employeeId: input.employeeId.trim(),
    name: input.name.trim(),
    email: input.email.trim(),
    phone: input.phone.trim(),
    hostelId: input.hostelId,
    password: input.password,
    assignedFloors: input.assignedFloors,
  };
}

/**
 * Executes POST /api/dgm/wardens and surfaces backend validation messages on failure.
 */
export async function submitCreateWardenRequest(
  fetchFn: typeof fetch,
  apiBaseUrl: string,
  headers: HeadersInit,
  data: CreateWardenFormInput
) {
  const res = await fetchFn(`${apiBaseUrl}/dgm/wardens`, {
    method: 'POST',
    headers,
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || 'Failed to create warden');
  }
  return res.json();
}
