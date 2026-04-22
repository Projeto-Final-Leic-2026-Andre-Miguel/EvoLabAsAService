import { API_BASE_URL, request, type RequestResult } from "../../api/api";
import type {LLMCredentials, CreateLLMCredentialRequest, CreateLocalModelCredentialRequest} from "../../types/credentials";

export const apiCredentials = {

    getAll(): Promise<RequestResult<LLMCredentials[]>> {
        return request<LLMCredentials[]>(`${API_BASE_URL}/llm-credentials`, {
            method: "GET"
        });
    },

    create(input: CreateLLMCredentialRequest): Promise<RequestResult<LLMCredentials>> {
        return request<LLMCredentials>(`${API_BASE_URL}/llm-credentials`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(input),
        });
    },

    createLocalModel(input: CreateLocalModelCredentialRequest): Promise<RequestResult<LLMCredentials>> {
        return request<LLMCredentials>(`${API_BASE_URL}/llm-credentials/localModel`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(input),
        });
    },

    delete(id: number): Promise<RequestResult<void>> {
        return request<void>(`${API_BASE_URL}/llm-credentials/${id}`, {
            method: "DELETE"
        });
    },

    update(id: number, input: CreateLLMCredentialRequest): Promise<RequestResult<LLMCredentials>> {
        return request<LLMCredentials>(`${API_BASE_URL}/llm-credentials/${id}`, {
            method: "PUT",
            headers: { 
                "Content-Type": "application/json"
            },
            body: JSON.stringify(input),
        });
    },

    validate(id: number): Promise<RequestResult<{ isValid: boolean }>> {
        return request<{ isValid: boolean }>(`${API_BASE_URL}/llm-credentials/${id}/validate`, {
            method: "POST"
        });
    }
};