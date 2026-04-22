export type LLM = "OPENAI" | "GEMINI" | "LOCAL_MODEL";
export interface LLMCredentials {
    id: number;
    userId: number;
    llm: LLM;
    apiKeyEncrypted?: string;
    createdAt?: string;

    // Added for frontend display
    apiKeyPreview?: string;
}

export interface CreateLLMCredentialRequest {
    llm: LLM;
    apiKey: string;
}

export interface CreateLocalModelCredentialRequest {
    llm: "LOCAL_MODEL";
    apiKey: string;
    port: number;
    modelName: string;
}
