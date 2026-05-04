export const ErrorDescriptions: Record<string, string> = {
    // LLM Credentials Errors
    "invalid-llm-provider": "The provided LLM provider is not valid.",
    "invalid-api-key": "The provided API key is invalid.",
    "llm-credentials-not-found": "The specified credentials were not found.",
    "credential-with-provider-already-in-use": "Credentials for this provider are already registered.",
    
    // Core Domain Errors
    "persistence-error": "An error occurred while saving the information. Please try again later.",
    "unknown-error": "An unknown error has occurred.",
    "unauthorized-access": "Unauthorized access. Please log in again.",

    // Project Errors
    "project-not-found": "The selected project was not found.",
    "config-not-found": "The configurations were not found.",
    "invalid-project-input": "The information provided for the project is invalid.",
    "not-project-owner": "Only the project owner can perform this action.",
    "config-access-denied": "Access to configurations denied.",
    "duplicate-project-name": "A project with this name already exists.",
    "invalid-project-status": "The current status of the project does not allow this action.",
    "execution-queue-unavailable": "The execution queue is currently unavailable."
};

/**
 * Helper to extract the corresponding error message in English from the error response
 */
export function getErrorMessage(errorTitle: string): string {
    if (!errorTitle) return "An unexpected error occurred.";
    // errorTitle is usually the end of the URI: "invalid-api-key", etc.
    return ErrorDescriptions[errorTitle] || "An unexpected error occurred. Please try again.";
}
