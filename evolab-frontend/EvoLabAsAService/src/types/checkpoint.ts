export interface Checkpoint {
    id: number;
    jobId: number;
    metricsId: number;
    iteration: number;
    solution: string;
    createdAt: string; // ISO 8601 Instant
}

