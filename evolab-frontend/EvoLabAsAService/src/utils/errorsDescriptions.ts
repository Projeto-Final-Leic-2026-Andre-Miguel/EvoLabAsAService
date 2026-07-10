export const ErrorDescriptions: Record<string, string> = {
    // LLM Credentials Errors
    "invalid-llm-provider": "The provided LLM provider is not valid.",
    "invalid-api-key": "The provided API key is invalid.",
    "llm-credentials-not-found": "The specified credentials were not found.",
    "credential-with-provider-already-in-use": "Credentials for this provider are already registered.",
    "credential-linked-to-configuration": "This credential is linked to a configuration. Delete the associated configuration before deleting the credential.",
    
    // Core Domain Errors
    "persistence-error": "An error occurred while saving the information. Please try again later.",
    "unknown-error": "An unknown error has occurred.",
    "unauthorized-access": "Unauthorized access. Please log in again.",

    // Project Errors
    "project-not-found": "The selected project was not found.",
    "config-not-found": "The configurations were not found.",
    "invalid-project-input": "",
    "not-project-owner": "Only the project owner can perform this action.",
    "config-access-denied": "Access to configurations denied.",
    "duplicate-project-name": "A project with this name already exists.",
    "invalid-project-status": "",
    "execution-queue-unavailable": "The execution queue is currently unavailable.",
    "invalid-config-input": "",

    // Model name errors (custom model support)
    "model-not-found": "The model name was not recognized by the provider. Check the exact model id in your Configurations.",
    "invalid-model-name": "The model name has an invalid format. Check it in your Configurations."
};

/**
 * Helper to extract the corresponding error message in English from the error response
 */
type ProblemError = {
    title?: string;
    detail?: string | null;
    key?: string;
};

export function getErrorMessage(error?: ProblemError | string | null): string {
    const title = typeof error === "string" ? error : error?.title || error?.key || "";
    const detail = typeof error === "string" ? null : error?.detail;
    return ErrorDescriptions[title] || detail || "An unexpected error occurred. Please try again.";
}
