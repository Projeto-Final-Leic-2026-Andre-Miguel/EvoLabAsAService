export enum LLM {
    OPENAI = 'OPENAI',
    GEMINI = 'GEMINI',
    LOCAL_MODEL = 'LOCAL_MODEL',
}

export interface LLMCredentials {
    id: number;
    userId: number;
    llm: LLM;
    apiKeyEncrypted: string | null;
    createdAt: string; // ISO 8601 Instant
}

export interface CreateLLMCredentialRequest {
    llm: LLM;
    apiKey: string;
}

