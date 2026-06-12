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

function readCookie(name: string): string | undefined {
    if (typeof document === "undefined") {
        return undefined;
    }

    const prefix = `${name}=`;
    return document.cookie
        .split(";")
        .map((cookie) => cookie.trim())
        .find((cookie) => cookie.startsWith(prefix))
        ?.slice(prefix.length);
}

function isUnsafeMethod(method: string | undefined): boolean {
    return !["GET", "HEAD", "OPTIONS", "TRACE"].includes((method ?? "GET").toUpperCase());
}

class ApiError extends Error {
    status: number;
    title: string;
    detail: string | null;
    key: string;

    constructor(status: number, title = "", detail: string | null = null, fallbackMessage = "") {
        super(detail || title || fallbackMessage || "An unexpected error occurred.");
        this.status = status;
        this.title = title;
        this.detail = detail;
        this.key = title;
    }
}

export async function request<T>(url: string, options?: RequestInit): Promise<RequestResult<T>> {
    try {
        const headers = new Headers(options?.headers);
        const csrfToken = readCookie("XSRF-TOKEN");

        if (csrfToken && isUnsafeMethod(options?.method) && !headers.has("X-XSRF-TOKEN")) {
            headers.set("X-XSRF-TOKEN", decodeURIComponent(csrfToken));
        }

        const response = await fetch(url, {
            credentials: "include",
            ...options,
            headers,
        });
        if (!response.ok) {
            const errorBody = await response.json().catch(() => ({}));
            const title = typeof errorBody.title === "string" ? errorBody.title : "";
            const detail = typeof errorBody.detail === "string" ? errorBody.detail : null;
            return {
                type: "Failure",
                data: null,
                error: new ApiError(response.status, title, detail, response.statusText)
            };
        }
        if (response.status === 204) {
            return {type: "Success", data: null as T, error: null };
        }
        const contentLength = response.headers.get("Content-Length");
        if (contentLength === '0') {
            return {type: "Success", data: null as T, error: null };
        }
        let data: T;
        try {
            data = await response.json();
        } catch {
            return {type: "Success", data: null as T, error: null };
        }

        return {type: "Success", data, error: null };
    } catch (e) {
        const message = e instanceof Error ? e.message : "A network error occurred.";
        return {
            type: "Failure",
            data: null,
            error: new ApiError(0, "", message)
        };
    }
}

export { ApiError };
