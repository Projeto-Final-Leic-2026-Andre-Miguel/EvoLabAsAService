import { describe, expect, it } from 'vitest';
import { getCredentialLabel } from './credentialLabels';

describe('getCredentialLabel', () => {
  it('uses only the human-readable provider name', () => {
    expect(getCredentialLabel({ llm: 'OPENAI' })).toBe('OpenAI');
    expect(getCredentialLabel({ llm: 'GEMINI' })).toBe('Gemini');
    expect(getCredentialLabel({ llm: 'ANTHROPIC' })).toBe('Anthropic');
    expect(getCredentialLabel({ llm: 'LOCAL_MODEL' })).toBe('Local Model');
  });
});
