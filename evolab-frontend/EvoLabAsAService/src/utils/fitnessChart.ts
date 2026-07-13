export const BAR_MAX_HEIGHT = 116;
const BAR_MIN_VISIBLE_HEIGHT = 8;

export function getFitnessScaleMax(scores: number[]): number {
  const finiteScores = scores.filter(Number.isFinite);
  return Math.max(0, ...finiteScores);
}

export function getFitnessBarHeight(score: number, maxScore: number): number {
  if (!Number.isFinite(score) || !Number.isFinite(maxScore) || score <= 0 || maxScore <= 0) return 0;

  const ratio = Math.min(1, Math.max(0, score / maxScore));
  return Math.max(BAR_MIN_VISIBLE_HEIGHT, ratio * BAR_MAX_HEIGHT);
}
