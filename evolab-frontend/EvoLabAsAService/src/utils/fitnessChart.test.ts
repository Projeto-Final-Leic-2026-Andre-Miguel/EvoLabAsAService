import { describe, expect, it } from 'vitest';
import { BAR_MAX_HEIGHT, getFitnessBarHeight, getFitnessScaleMax } from './fitnessChart';

describe('fitness chart scaling', () => {
  it('uses zero as the scale maximum for empty or non-positive scores', () => {
    expect(getFitnessScaleMax([])).toBe(0);
    expect(getFitnessScaleMax([0, -1, -10])).toBe(0);
  });

  it('keeps zero and negative scores at zero height', () => {
    expect(getFitnessBarHeight(0, 1)).toBe(0);
    expect(getFitnessBarHeight(-0.5, 1)).toBe(0);
  });

  it('normalizes positive scores without exceeding the visual maximum', () => {
    expect(getFitnessBarHeight(0.5, 1)).toBe(BAR_MAX_HEIGHT / 2);
    expect(getFitnessBarHeight(1, 1)).toBe(BAR_MAX_HEIGHT);
    expect(getFitnessBarHeight(100, 1)).toBe(BAR_MAX_HEIGHT);
  });

  it('preserves a visible minimum for small positive scores', () => {
    expect(getFitnessBarHeight(0.001, 100)).toBe(8);
  });
});
