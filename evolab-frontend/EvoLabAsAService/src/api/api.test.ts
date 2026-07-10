import { afterEach, describe, expect, it, vi } from 'vitest';
import { request } from './api';

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('request', () => {
  it('preserves the backend problem title and detail', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          title: 'persistence-error',
          detail: 'Could not delete credential.',
        }),
        {
          status: 500,
          headers: { 'Content-Type': 'application/problem+json' },
        },
      ),
    ));

    const result = await request('/api/example');

    expect(result.type).toBe('Failure');
    if (result.type === 'Failure') {
      expect(result.error.message).toBe('Could not delete credential.');
      expect(result.error.title).toBe('persistence-error');
      expect(result.error.detail).toBe('Could not delete credential.');
    }
  });
});
