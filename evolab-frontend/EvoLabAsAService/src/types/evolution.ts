export const EvolutionStatus = {
    CREATED: 'CREATED',
    QUEUED: 'QUEUED',
    RUNNING: 'RUNNING',
    COMPLETED: 'COMPLETED',
    FAILED: 'FAILED',
} as const;

export type EvolutionStatus = typeof EvolutionStatus[keyof typeof EvolutionStatus];

