import { describe, expect, it } from 'vitest';
import { getCredentialLabel } from './credentialLabels';

describe('getCredentialLabel', () => {
  it('uses the human-readable provider name and credential id', () => {
    expect(getCredentialLabel({ id: 5, llm: 'OPENAI' })).toBe('OpenAI credential #5');
    expect(getCredentialLabel({ id: 8, llm: 'LOCAL_MODEL' })).toBe('Local Model credential #8');
  });
});
