export const API_BASE_URL = "/api";

export type RequestResult<T> = Success<T> | Failure

export type Success<T> = {type: "Success", data: T; error: null } 

export type Failure = { type: "Failure", data: null; error: ApiError }

export function onResult<T>(result: RequestResult<T>, onSuccess: (result: Success<T>) => void, onFailure: (result: Failure)=> void): void {
    switch (result.type) {
        case "Success": onSuccess(result); break;
        case "Failure": onFailure(result); break;
    }
}

class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

export async function request<T>(url: string, options?: RequestInit): Promise<RequestResult<T>> {
    try {
        const response = await fetch(url, {
            credentials: "include",
            ...options
        });
        if (!response.ok) {
            const errorBody = await response.json().catch(() => ({}));
            const errorMessage = errorBody.title || errorBody.detail || response.statusText;
            return {
                type: "Failure",
                data: null,
                error: new ApiError(response.status, errorMessage)
            };
        }
        if (response.status === 204) {
            return {type: "Failure", data: null, error: new ApiError(0, "IS EMPTY") };
        }
        const contentLength = response.headers.get("Content-Length");
        if (contentLength === '0') {
            return {type: "Success", data: null as T, error: null };
        }
        const data = await response.json();

        return {type: "Success", data, error: null };
    } catch (e) {
        const message = e instanceof Error ? e.message : "A network error occurred.";
        return {
            type: "Failure",
            data: null,
            error: new ApiError(0, message)
        };
    }
}

export { ApiError };