import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiFetch } from './api';

describe('apiFetch', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('unwraps successful ApiResponse payloads', async () => {
    vi.mocked(fetch).mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ success: true, data: { ok: true }, error: null }),
    } as Response);

    await expect(apiFetch<{ ok: boolean }>('/actuator/health')).resolves.toEqual({ ok: true });
  });

  it('throws backend error messages', async () => {
    vi.mocked(fetch).mockResolvedValue({
      ok: false,
      status: 400,
      json: async () => ({ success: false, data: null, error: { code: 'VALIDATION_ERROR', message: 'bad' } }),
    } as Response);

    await expect(apiFetch('/bad')).rejects.toThrow('bad');
  });
});
