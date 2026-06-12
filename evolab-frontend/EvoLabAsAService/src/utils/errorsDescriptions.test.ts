import { describe, expect, it } from 'vitest';
import { getErrorMessage } from './errorsDescriptions';

describe('getErrorMessage', () => {
  it('prefers friendly text mapped from the problem title', () => {
    expect(getErrorMessage({
      title: 'credential-with-provider-already-in-use',
      detail: "User already has credentials for provider 'OPENAI'",
    })).toBe('Credentials for this provider are already registered.');
  });

  it('uses the backend detail when the title has no friendly text', () => {
    expect(getErrorMessage({
      title: 'invalid-project-input',
      detail: 'Project name cannot be blank',
    })).toBe('Project name cannot be blank');
  });

  it('falls back when neither title nor detail is available', () => {
    expect(getErrorMessage({ title: '', detail: '' })).toBe('An unexpected error occurred. Please try again.');
  });
});
