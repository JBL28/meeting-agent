import { apiFetch } from './api';
import type { AuthResponse, Member } from './domain';

export function register(input: { email: string; password: string; name: string }) {
  return apiFetch<Member>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function login(input: { email: string; password: string }) {
  return apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function getMe() {
  return apiFetch<Member>('/api/auth/me');
}
