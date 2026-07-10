import type { LLM } from '../types/credentials';

const PROVIDER_LABELS: Record<LLM, string> = {
  OPENAI: 'OpenAI',
  GEMINI: 'Google Gemini',
  ANTHROPIC: 'Anthropic Claude',
  LOCAL_MODEL: 'Local Model',
};

export function getProviderLabel(llm: LLM): string {
  return PROVIDER_LABELS[llm];
}

export function getCredentialLabel(credential: { id: number; llm: LLM }): string {
  return `${getProviderLabel(credential.llm)} credential #${credential.id}`;
}
