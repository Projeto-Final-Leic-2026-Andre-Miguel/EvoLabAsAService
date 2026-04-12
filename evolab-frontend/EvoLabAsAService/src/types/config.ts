export interface Config {
    id: number;
    userId: number;
    llmCredentialsId: number;
    modelName: string;
    maxIter: number;
    checkPointInterval: number;
    additionalParams: Record<string, string>;
    createdAt: string; // ISO 8601 Instant
}

export interface CreateConfigInput {
    projectId: number | null;
    llmCredentialsId: number;
    modelName: string;
    maxIter: number;
    checkPointInterval: number;
    additionalParams: Record<string, string>;
}

export interface UpdateConfigInput {
    modelName: string;
    maxIter: number;
    checkPointInterval: number;
    additionalParams: Record<string, string>;
}

export interface UserConfigOutput {
    configId: number;
    projectId: number | null;
    userId: number;
    llmCredentialsId: number;
    modelName: string;
    maxIter: number;
    checkPointInterval: number;
    additionalParams: Record<string, string>;
    createdAt: string; // ISO 8601 Instant
}

export interface GenerateTemporaryConfigFileInput {
    projectId: number;
    jobId: number | null;
}

export interface TemporaryConfigFileOutput {
    path: string;
}

