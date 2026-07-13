import { describe, expect, it } from 'vitest';
import { getMissingProjectRequirements, getProjectActionPermissions } from './projectReadiness';

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

describe('getProjectActionPermissions', () => {
  it.each(['QUEUED', 'RUNNING'] as const)('disables mutating actions while %s', (status) => {
    expect(getProjectActionPermissions(status)).toEqual({
      details: true,
      start: false,
      restart: false,
      update: false,
      delete: false,
    });
  });

  it('allows starting and editing a created project', () => {
    expect(getProjectActionPermissions('CREATED')).toEqual({
      details: true,
      start: true,
      restart: false,
      update: true,
      delete: true,
    });
  });

  it.each(['COMPLETED', 'FAILED'] as const)('allows restart, update and delete while %s', (status) => {
    expect(getProjectActionPermissions(status)).toEqual({
      details: true,
      start: false,
      restart: true,
      update: true,
      delete: true,
    });
  });
});
