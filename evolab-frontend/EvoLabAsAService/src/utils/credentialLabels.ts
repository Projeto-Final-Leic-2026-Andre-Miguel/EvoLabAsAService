import type { LLM } from '../types/credentials';

const PROVIDER_LABELS: Record<LLM, string> = {
  OPENAI: 'OpenAI',
  GEMINI: 'Gemini',
  ANTHROPIC: 'Anthropic',
  LOCAL_MODEL: 'Local Model',
};

export function getProviderLabel(llm: LLM): string {
  return PROVIDER_LABELS[llm];
}

export function getCredentialLabel(credential: { llm: LLM }): string {
  return getProviderLabel(credential.llm);
}
