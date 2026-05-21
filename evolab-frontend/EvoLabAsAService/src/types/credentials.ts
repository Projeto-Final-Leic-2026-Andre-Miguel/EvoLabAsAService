export type LLM = "OPENAI" | "GEMINI" | "ANTHROPIC" | "LOCAL_MODEL";
export interface LLMCredentials {
    id: number;
    userId: number;
    llm: LLM;
    apiKeyEncrypted?: string;
    createdAt?: string;

    // Added for frontend display
    apiKeyPreview?: string;
    modelName?: string;
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
