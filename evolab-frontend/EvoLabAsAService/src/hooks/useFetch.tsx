import { useEffect, useReducer } from "react";
import { request, type RequestResult } from "../api/api";

// Generic State type
type State<T> =
    | { type: "begin" }
    | { type: "loading"; url: string }
    | { type: "loaded"; payload: T; url: string }
    | { type: "error"; error: Error; url: string };

// Generic Action type
type Action<T> =
    | { type: "load"; url: string }
    | { type: "success"; payload: T; url: string }
    | { type: "reset" };

function unexpectedAction<T>(action: Action<T>, state: State<T>) {
    console.log(`Unexpected action ${action.type} in state ${state.type}`);
    return state;
}

// Generic reducer
function reducer<T>(state: State<T>, action: Action<T>): State<T> {
    switch (action.type) {
        case "reset":
            return { type: "begin" };
        case "load":
            return { type: "loading", url: action.url };
        case "success":
            if (state.type !== "loading" || state.url !== action.url) {
                return unexpectedAction(action, state);
            }
        return { type: "loaded", payload: action.payload, url: state.url };
    }
}

// Options for the hook
interface UseFetchOptions {
    headers?: HeadersInit;
    method?: string;
    body?: string;
}

const firstState = { type: "begin" } as const;

export function useFetch<T>(
    url: string | undefined,
    options?: UseFetchOptions,
    dependency?: any
): State<T> {
    const [state, dispatch] = useReducer(reducer<T>, firstState as State<T>);

    useEffect(() => {
        if (!url) {
            dispatch({ type: "reset" });
            return;
        }

        const urlToUse = url;
        let cancelled = false;
        const abortController = new AbortController();
        async function doFetch() {
            dispatch({ type: "load", url: urlToUse });
            try {
                const response: RequestResult<T> = await request<T>(urlToUse, {
                    ...options,
                    signal: abortController.signal,
                });

                if (cancelled) return;

                if (response.data !== undefined && response.data !== null) {
                    dispatch({ type: "success", payload: response.data, url: urlToUse });
                } else {
                    // Handle cases where data is null/undefined without an explicit error
                    dispatch({ type: "reset" });
                }

            } catch (error) {
                if (!cancelled) {
                    console.error(error instanceof Error ? error : new Error("An error occurred"));
                    dispatch({ type: "reset" });
                }
            }
        }

        doFetch();

        return () => {
            cancelled = true;
            abortController.abort();
        };
    }, [url, options?.method, options?.body, JSON.stringify(options?.headers),dependency]); 

    return state;
}