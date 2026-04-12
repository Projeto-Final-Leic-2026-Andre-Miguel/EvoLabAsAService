import { EvolutionStatus } from './evolution';

export interface Project {
    id: number;
    userId: number;
    configId: number | null;
    name: string;
    description: string | null;
    initialProgram: string | null;
    evaluatorCode: string | null;
    status: EvolutionStatus;
    createdAt: string; // ISO 8601 Instant
}

export interface CreateProjectInput {
    name: string;
    description: string | null;
    configId: number | null;
    initialProgram: string | null;
    evaluatorCode: string | null;
}

export interface UpdateProjectDetailsInput {
    name: string | null;
    description: string | null;
    configId: number | null;
    initialProgram: string | null;
    evaluatorCode: string | null;
}

export interface UpdateProjectStatusInput {
    status: EvolutionStatus;
}

