import { EvolutionStatus } from './evolution';

export interface Job {
    id: number;
    projectId: number;
    status: EvolutionStatus;
    containerId: string | null;
    startedAt: string | null; // ISO 8601 Instant
    finishedAt: string | null; // ISO 8601 Instant
    bestSolution: string | null;
    executionLogs: string | null;
    createdAt: string; // ISO 8601 Instant
}

