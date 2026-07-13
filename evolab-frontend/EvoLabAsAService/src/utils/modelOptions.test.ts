import { describe, expect, it } from 'vitest';
import { OPENAI_MODELS, isPredefinedModel } from './modelOptions';

describe('OpenAI model options', () => {
  it('offers o3-mini by alias without replacing gpt-4o-mini', () => {
    expect(OPENAI_MODELS).toContain('gpt-4o-mini');
    expect(OPENAI_MODELS).toContain('o3-mini');
    expect(OPENAI_MODELS).not.toContain('o3-mini-2025-01-31');
    expect(OPENAI_MODELS[0]).not.toBe('o3-mini');
    expect(isPredefinedModel('OPENAI', 'o3-mini')).toBe(true);
  });
});
