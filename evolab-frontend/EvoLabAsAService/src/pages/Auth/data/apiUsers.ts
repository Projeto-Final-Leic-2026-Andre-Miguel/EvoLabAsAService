import type {
    User,
    CreateLocalUserInput,
    CreateTokenInput,
    TokenExternalInfo
} from "../../../types/user";

import { API_BASE_URL, request, type RequestResult } from "../../../api/api";

export const apiUsers = {

    login(input: CreateTokenInput): Promise<RequestResult<void>> {
        return request<void>(`${API_BASE_URL}/users/token`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(input),
        });
    },

    generateToken(): Promise<RequestResult<TokenExternalInfo>> {
        return request<TokenExternalInfo>(`${API_BASE_URL}/users/token/generate`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
        });
    },

    createLocalUser(input: CreateLocalUserInput): Promise<RequestResult<User>> {
        return request<User>(`${API_BASE_URL}/users/local`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(input),
        });
    },

    logout(): Promise<RequestResult<void>> {
        return request<void>(`${API_BASE_URL}/logout`, {
            method: "POST",
            headers: { 
                "Content-Type": "application/json"
            }
        });
    },

    userHome(token: string): Promise<RequestResult<User>> {
        return request<User>(`${API_BASE_URL}/me`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
    },

    getAllUsers(token: string): Promise<RequestResult<User[]>> {
        return request<User[]>(`${API_BASE_URL}/users`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });
    },

    deleteUser(id: number): Promise<RequestResult<void>> {
        return request<void>(`${API_BASE_URL}/users/${id}`, {
            method: "DELETE"
        });
    },
    
    loginWithGoogle(): void {
        // Usa o URL relativo que vai ser intercetado e manipulado pelo dev proxy do Vite (/oauth2)
        // Desta forma, manténs o teu browser na mesma "origem" (localhost:5173) limitando problemas de cookie
        window.location.href = "/oauth2/authorization/google";
    }
    
}
