export type ApiError = {
  code: string;
  message: string;
};

export type ApiResponse<T> = {
  success: boolean;
  data: T | null;
  error: ApiError | null;
};

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? 'http://localhost:8080';

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = typeof window !== 'undefined' ? window.localStorage.getItem('accessToken') : null;
  const headers = new Headers(init.headers);
  if (!(init.body instanceof FormData)) {
    headers.set('Content-Type', headers.get('Content-Type') ?? 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
  });

  if (response.status === 401 && typeof window !== 'undefined') {
    window.localStorage.removeItem('accessToken');
    window.location.assign('/login');
  }

  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || !body.success) {
    throw new Error(body.error?.message ?? `HTTP ${response.status}`);
  }
  return body.data as T;
}
