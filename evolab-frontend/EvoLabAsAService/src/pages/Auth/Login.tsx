import React, { useReducer } from "react";
import { useNavigate } from "react-router-dom";
import { onResult } from "../../api/api";
import { apiUsers } from "./data/apiUsers";
import styles from "./Login.module.css";
import { useAuth } from "../../contexts/AuthContext";

type Stage = "editing" | "posting" | "success" | "failed";

type State = {
    email: string;
    password: string;
    error: string | undefined;
    stage: Stage;
};

type Action =
    | { type: "input-change"; email: string; password: string }
    | { type: "post" }
    | { type: "success" }
    | { type: "error"; message: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "input-change":
            return { ...state, email: action.email, password: action.password };
        case "post":
            return { ...state, stage: "posting", error: undefined };
        case "success":
            return { ...state, email: "", password: "", stage: "success", error: undefined };
        case "error":
            return { ...state, stage: "failed", error: action.message };
    }
}

const initialState: State = {
    email: "",
    password: "",
    error: undefined,
    stage: "editing",
};

export function Login() {
    const [state, dispatch] = useReducer(reducer, initialState);
    const navigate = useNavigate();
    const { reload } = useAuth(); // Importa o reload do authContext

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        dispatch({ type: "post" });
        onResult(
            await apiUsers.login({ email: state.email, password: state.password }),
            (success) => {
                dispatch({ type: "success" });
                reload(); // Atualiza o estado global para reconhecer o cookie recém-adquirido
                navigate("/"); 
            },
            (failure) => {
                const errorType = failure.error?.message;
                dispatch({ type: "error", message: errorType || "Erro ao fazer login" });
            }
        );
    };

    return (
        <div className={styles.authContainer}>
            <div className={styles.authCard}>
                <h2>Sign in</h2>
                <form onSubmit={handleSubmit}>
                    <label className={styles.inputLabel}>
                        Email:
                        <input
                            type="email"
                            value={state.email}
                            onChange={(e) =>
                                dispatch({
                                    type: "input-change",
                                    email: e.target.value,
                                    password: state.password,
                                })
                            }
                            required
                        />
                    </label>
                    <label className={styles.inputLabel}>
                        Password:
                        <input
                            type="password"
                            value={state.password}
                            onChange={(e) =>
                                dispatch({
                                    type: "input-change",
                                    email: state.email,
                                    password: e.target.value,
                                })
                            }
                            required
                        />
                    </label>
                    {state.error && <div className={styles.errorMsg}>{state.error}</div>}
                    <button type="submit" disabled={state.stage === "posting"} className={styles.submitBtn}>
                        {state.stage === "posting" ? "Verifing..." : "Sign in"}
                    </button>
                </form>

                <div className={styles.divider}>
                    <span>or</span>
                </div>

                <button
                    type="button"
                    onClick={apiUsers.loginWithGoogle}
                    className={styles.googleBtn}
                >
                    <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg">
                        <g transform="matrix(1, 0, 0, 1, 27.009001, -39.238998)">
                            <path fill="#4285F4" d="M -3.264 51.509 C -3.264 50.719 -3.334 49.969 -3.454 49.239 L -14.754 49.239 L -14.754 53.749 L -8.284 53.749 C -8.574 55.229 -9.424 56.479 -10.684 57.329 L -10.684 60.329 L -6.824 60.329 C -4.564 58.239 -3.264 55.159 -3.264 51.509 Z"/>
                            <path fill="#34A853" d="M -14.754 63.239 C -11.514 63.239 -8.804 62.159 -6.824 60.329 L -10.684 57.329 C -11.764 58.049 -13.134 58.489 -14.754 58.489 C -17.884 58.489 -20.534 56.379 -21.484 53.529 L -25.464 53.529 L -25.464 56.619 C -23.494 60.539 -19.444 63.239 -14.754 63.239 Z"/>
                            <path fill="#FBBC05" d="M -21.484 53.529 C -21.734 52.809 -21.864 52.039 -21.864 51.239 C -21.864 50.439 -21.724 49.669 -21.484 48.949 L -21.484 45.859 L -25.464 45.859 C -26.284 47.479 -26.754 49.299 -26.754 51.239 C -26.754 53.179 -26.284 54.999 -25.464 56.619 L -21.484 53.529 Z"/>
                            <path fill="#EA4335" d="M -14.754 43.989 C -12.984 43.989 -11.404 44.599 -10.154 45.789 L -6.734 41.939 C -8.804 40.009 -11.514 38.989 -14.754 38.989 C -19.444 38.989 -23.494 41.689 -25.464 45.859 L -21.484 48.949 C -20.534 46.099 -17.884 43.989 -14.754 43.989 Z"/>
                        </g>
                    </svg>
                    Sign in with Google
                </button>

                <p className={styles.loginPrompt}>
                    Don´t have an account? <span onClick={() => navigate("/register")} className={styles.link}>Register through here</span>
                </p>
            </div>
        </div>
    );
}

