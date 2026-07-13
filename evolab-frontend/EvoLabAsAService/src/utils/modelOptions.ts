import type { LLM } from '../types/credentials';

export const OPENAI_MODELS = ['gpt-4.1-mini', 'gpt-4o-mini', 'o3-mini', 'gpt-5.6-sol', 'gpt-5.6-terra', 'gpt-5.6-luna'];
export const GEMINI_MODELS = ['gemini-3.1-flash-lite', 'gemini-2.5-pro', 'gemini-2.5-flash', 'gemini-2.5-flash-lite', 'gemini-3.5-flash'];
export const ANTHROPIC_MODELS = ['claude-haiku-4-5-20251001', 'claude-sonnet-4-6', 'claude-opus-4-6'];

export function isPredefinedModel(llm: LLM | undefined, value: string): boolean {
  if (!llm || !value) return false;
  if (llm === 'OPENAI') return OPENAI_MODELS.includes(value);
  if (llm === 'GEMINI') return GEMINI_MODELS.includes(value);
  if (llm === 'ANTHROPIC') return ANTHROPIC_MODELS.includes(value);
  return true;
}
