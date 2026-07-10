import { describe, expect, it } from 'vitest';
import { validateConfigValues } from './configValidation';

const validParams = {
  'llm.top_p': '0.9',
  'llm.max_tokens': '100',
  'llm.timeout': '30',
  'llm.retries': '2',
  'prompt.num_top_programs': '3',
  'prompt.num_diverse_programs': '2',
  'database.population_size': '10',
  'database.archive_size': '5',
  'database.num_islands': '2',
  'database.migration_interval': '4',
  'database.migration_rate': '0.1',
  'database.elite_selection_ratio': '0.1',
  'database.exploration_ratio': '0.2',
  'database.exploitation_ratio': '0.7',
  'database.feature_bins': '10',
  'evaluator.timeout': '60',
  'evaluator.max_retries': '2',
  'evaluator.parallel_evaluations': '2',
  'evaluator.cascade_evaluation': 'true',
  'evaluator.cascade_threshold_1': '0.2',
  'evaluator.cascade_threshold_2': '0.5',
  'evaluator.cascade_threshold_3': '0.8',
};

describe('validateConfigValues', () => {
  it('requires positive iteration values', () => {
    expect(validateConfigValues(0, 0, {}).maxIter).toBe('Max iterations must be greater than 0.');
    expect(validateConfigValues(1, 0, {}).checkPointInterval).toBe('Checkpoint interval must be greater than 0.');
  });

  it('validates ratios and positive sizes', () => {
    const errors = validateConfigValues(10, 5, {
      ...validParams,
      'llm.top_p': '1.2',
      'database.population_size': '0',
    });

    expect(errors['llm.top_p']).toBe('Top P must be between 0 and 1.');
    expect(errors['database.population_size']).toBe('Population size must be greater than 0.');
  });

  it('requires strictly increasing cascade thresholds', () => {
    const errors = validateConfigValues(10, 5, {
      ...validParams,
      'evaluator.cascade_threshold_2': '0.2',
    });

    expect(errors['evaluator.cascade_threshold_2']).toContain('strictly increasing');
  });

  it('accepts valid values', () => {
    expect(validateConfigValues(10, 5, validParams)).toEqual({});
  });
});
