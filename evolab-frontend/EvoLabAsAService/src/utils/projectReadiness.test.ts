import { describe, expect, it } from 'vitest';
import { getMissingProjectRequirements } from './projectReadiness';

describe('getMissingProjectRequirements', () => {
  it('lists exactly the missing start requirements', () => {
    expect(getMissingProjectRequirements({
      configId: null,
      initialProgram: 'print("ready")',
      evaluatorCode: ' ',
    })).toEqual(['configuration', 'evaluator code']);
  });

  it('returns no missing requirements for a ready project', () => {
    expect(getMissingProjectRequirements({
      configId: 4,
      initialProgram: 'print("ready")',
      evaluatorCode: 'def evaluate(): pass',
    })).toEqual([]);
  });
});
