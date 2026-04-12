export interface Metric {
    id: number;
    jobId: number;
    iteration: number;
    fitnessScore: number;
    executionTime: number | null;
    createdAt: string; // ISO 8601 Instant
}

