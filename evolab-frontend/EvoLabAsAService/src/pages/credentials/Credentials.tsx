import React, { useEffect, useReducer } from "react";
import { motion } from "framer-motion";
import styles from "./Credentials.module.css";
import { apiCredentials } from "./apiCredentials";
import type { LLM, LLMCredentials } from "../../types/credentials";
import { useAuth } from "../../contexts/AuthContext";
import { useValidCredentials } from "../../contexts/ValidCredentialsContext";
import { ErrorDescriptions } from "../../utils/errorsDescriptions";

const PROVIDER_INFO: Record<LLM, { icon: string, bgClass: string, label: string }> = {
    OPENAI: { icon: "O", bgClass: styles.bgOpenAI, label: "OpenAI" },
    GEMINI: { icon: "G", bgClass: styles.bgGemini, label: "Google Gemini" },
    LOCAL_MODEL: { icon: "L", bgClass: styles.bgOllama, label: "Local Model" },
};

type State = {
    credentials: LLMCredentials[];
    isLoading: boolean;
    isModalOpen: boolean;
    editId: number | null;
    provider: LLM;
    apiKey: string;
    isSubmitting: boolean;
    validatingId: number | null;
    errorMessage: string | null;
};

type Action =
    | { type: "FETCH_START" }
    | { type: "FETCH_SUCCESS"; payload: LLMCredentials[] }
    | { type: "OPEN_MODAL_CREATE" }
    | { type: "OPEN_MODAL_EDIT"; payload: LLMCredentials }
    | { type: "CLOSE_MODAL" }
    | { type: "SET_PROVIDER"; payload: LLM }
    | { type: "SET_API_KEY"; payload: string }
    | { type: "SUBMIT_START" }
    | { type: "SUBMIT_ERROR"; payload: string }
    | { type: "SUBMIT_SUCCESS_CREATE"; payload: LLMCredentials }
    | { type: "SUBMIT_SUCCESS_UPDATE"; payload: LLMCredentials }
    | { type: "DELETE_SUCCESS"; payload: number }
    | { type: "SET_ERROR"; payload: string | null }
    | { type: "SET_VALIDATING"; payload: number | null };

const initialState: State = {
    credentials: [],
    isLoading: true,
    isModalOpen: false,
    editId: null,
    provider: "OPENAI",
    apiKey: "",
    isSubmitting: false,
    validatingId: null,
    errorMessage: null,
};

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case "FETCH_START":
            return { ...state, isLoading: true, errorMessage: null };
        case "FETCH_SUCCESS":
            return { ...state, isLoading: false, credentials: action.payload };
        case "OPEN_MODAL_CREATE":
            return { ...state, isModalOpen: true, provider: "OPENAI", apiKey: "", editId: null, errorMessage: null };
        case "OPEN_MODAL_EDIT":
            return { ...state, isModalOpen: true, provider: action.payload.llm, apiKey: "", editId: action.payload.id, errorMessage: null };
        case "CLOSE_MODAL":
            return { ...state, isModalOpen: false, errorMessage: null };
        case "SET_PROVIDER":
            return { ...state, provider: action.payload };
        case "SET_API_KEY":
            return { ...state, apiKey: action.payload };
        case "SUBMIT_START":
            return { ...state, isSubmitting: true, errorMessage: null };
        case "SUBMIT_ERROR":
            return { ...state, isSubmitting: false, errorMessage: action.payload };
        case "SUBMIT_SUCCESS_CREATE":
            return { ...state, isSubmitting: false, isModalOpen: false, credentials: [...state.credentials, action.payload] };
        case "SUBMIT_SUCCESS_UPDATE":
            return { ...state, isSubmitting: false, isModalOpen: false, credentials: state.credentials.map(c => c.id === action.payload.id ? action.payload : c) };
        case "DELETE_SUCCESS":
            return { ...state, credentials: state.credentials.filter(c => c.id !== action.payload) };
        case "SET_ERROR":
            return { ...state, errorMessage: action.payload };
        case "SET_VALIDATING":
            return { ...state, validatingId: action.payload };
        default:
            return state;
    }
}

export function Credentials() {
    const { user } = useAuth();
    const { validCredentialsMap, validateCredential, setCredentialValidity } = useValidCredentials();

    const [state, dispatch] = useReducer(reducer, initialState);

    useEffect(() => {
        if (user) fetchCredentials();
    }, [user]);

    const getFriendlyErrorMessage = (errorKey: string, defaultMessage: string = "Ocorreu um erro desconhecido.") => {
        return ErrorDescriptions[errorKey] || defaultMessage;
    };

    const fetchCredentials = async () => {
        dispatch({ type: "FETCH_START" });
        const result = await apiCredentials.getAll();
        if (result.type === "Success") {
            result.data.forEach(c => {
                if (validCredentialsMap[c.id] === undefined) {
                    setCredentialValidity(c.id, true);
                }
            });
            dispatch({ type: "FETCH_SUCCESS", payload: result.data });
        } else {
            dispatch({ type: "FETCH_SUCCESS", payload: [] }); // or handle fetch error
        }
    };

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        dispatch({ type: "SUBMIT_START" });

        if (state.editId) {
            const res = await apiCredentials.update(state.editId, { llm: state.provider, apiKey: state.apiKey });
            if (res.type === "Success") {
                setCredentialValidity(res.data.id, true);
                dispatch({ type: "SUBMIT_SUCCESS_UPDATE", payload: res.data });
            } else {
                dispatch({ type: "SUBMIT_ERROR", payload: getFriendlyErrorMessage(res.error.message, "Erro ao atualizar credencial.") });
            }
        } else {
            const res = await apiCredentials.create({ llm: state.provider, apiKey: state.apiKey });
            if (res.type === "Success") {
                setCredentialValidity(res.data.id, true);
                dispatch({ type: "SUBMIT_SUCCESS_CREATE", payload: res.data });
            } else {
                dispatch({ type: "SUBMIT_ERROR", payload: getFriendlyErrorMessage(res.error.message, "Erro ao criar credencial.") });
            }
        }
    };

    const handleDelete = async (id: number) => {
        const res = await apiCredentials.delete(id);
        if (res.type === "Success" || res.error?.status === 0 || res.error?.status === 200 || !res.error) {
            dispatch({ type: "DELETE_SUCCESS", payload: id });
        } else {
            dispatch({ type: "SET_ERROR", payload: getFriendlyErrorMessage(res.error?.message ?? "", "Erro ao apagar credencial.") });
            setTimeout(() => dispatch({ type: "SET_ERROR", payload: null }), 5000);
        }
    };

    const handleValidate = async (id: number) => {
        dispatch({ type: "SET_VALIDATING", payload: id });
        const valid = await validateCredential(id);
        if (valid === null) {
            dispatch({ type: "SET_ERROR", payload: "Falha ao validar credencial com o servidor." });
            setTimeout(() => dispatch({ type: "SET_ERROR", payload: null }), 5000);
        }
        dispatch({ type: "SET_VALIDATING", payload: null });
    };

    const renderCreatedAt = (createdAt?: string) => {
        if (!createdAt) return "Unknown date";
        const n = Number(createdAt);
        return !isNaN(n) ? new Date(n * 1000).toLocaleDateString() : new Date(createdAt).toLocaleDateString();
    };

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <h1>API Credentials</h1>
                <button className={styles.createBtn} onClick={() => dispatch({ type: "OPEN_MODAL_CREATE" })}>
                    + New Credential
                </button>
            </div>
            
            {state.errorMessage && !state.isModalOpen && (
                <div style={{ padding: '1rem', backgroundColor: '#fee2e2', color: '#dc2626', borderRadius: '8px', marginBottom: '1rem' }}>
                    {state.errorMessage}
                </div>
            )}

            {state.isLoading ? (
                <div>Loading credentials...</div>
            ) : (
                <div className={styles.grid}>
                    {state.credentials.map(cred => {
                        const info = PROVIDER_INFO[cred.llm] || PROVIDER_INFO["OPENAI"];
                        return (
                            <motion.div 
                                className={styles.card} 
                                key={cred.id}
                                initial={{ opacity: 0, scale: 0.95 }}
                                animate={{ opacity: 1, scale: 1 }}
                            >
                                <div className={styles.cardTop}>
                                    <div className={`${styles.providerIcon} ${info?.bgClass || styles.bgOllama}`}>
                                        {info?.icon || "?"}
                                    </div>
                                    <h3 className={styles.providerName}>{info?.label || cred.llm}</h3>

                                    {state.validatingId === cred.id ? (
                                        <span className={`${styles.statusBadge} ${styles.statusNormal}`}>
                                            Validating...
                                        </span>
                                    ) : validCredentialsMap[cred.id] ? (
                                        <span className={`${styles.statusBadge} ${styles.statusValid}`}>Valid</span>
                                    ) : (
                                        <span className={`${styles.statusBadge} ${styles.statusInvalid}`}>Invalid</span>
                                    )}
                                </div>

                                <div className={styles.keyPreview} style={{ fontSize: '0.85rem' }}>
                                    <strong>Created: </strong> {renderCreatedAt(cred.createdAt)}
                                </div>

                                <div className={styles.actions}>
                                    <button onClick={() => handleValidate(cred.id)} className={`${styles.actionBtn} ${styles.validateBtn}`}>
                                        Validate
                                    </button>
                                    <button onClick={() => dispatch({ type: "OPEN_MODAL_EDIT", payload: cred })} className={`${styles.actionBtn} ${styles.updateBtn}`}>
                                        Update
                                    </button>
                                    <button onClick={() => handleDelete(cred.id)} className={`${styles.actionBtn} ${styles.deleteBtn}`}>
                                        Delete
                                    </button>
                                </div>
                            </motion.div>
                        );
                    })}
                </div>
            )}

            {state.isModalOpen && (
                <div className={styles.overlay}>
                    <motion.div 
                        className={styles.modal}
                        initial={{ opacity: 0, y: -50 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        <h2>{state.editId ? "Update Credential" : "New Credential"}</h2>
                        {state.errorMessage && (
                            <div style={{ color: '#dc2626', marginBottom: '1rem', fontSize: '0.9rem' }}>
                                {state.errorMessage}
                            </div>
                        )}
                        <form onSubmit={handleSave}>
                            <div className={styles.formGroup}>
                                <label>Provider</label>
                                <select 
                                    value={state.provider} 
                                    onChange={e => dispatch({ type: "SET_PROVIDER", payload: e.target.value as LLM })}
                                    className={styles.inputField}
                                    disabled={!!state.editId}
                                >
                                    <option value="OPENAI">OpenAI</option>
                                    <option value="GEMINI">Google Gemini</option>
                                    <option value="LOCAL_MODEL">Local Model</option>
                                </select>
                            </div>

                            <div className={styles.formGroup}>
                                <label>API Key</label>
                                <input 
                                    type="password" 
                                    required 
                                    placeholder="Enter your secret key"
                                    value={state.apiKey}
                                    onChange={e => dispatch({ type: "SET_API_KEY", payload: e.target.value })}
                                    className={styles.inputField}
                                />
                            </div>

                            <div className={styles.modalActions}>
                                <button type="button" onClick={() => dispatch({ type: "CLOSE_MODAL" })} className={styles.cancelBtn}>
                                    Cancel
                                </button>
                                <button type="submit" disabled={state.isSubmitting} className={styles.saveBtn}>
                                    {state.editId ? "Save Changes" : "Create"}
                                </button>
                            </div>
                        </form>
                    </motion.div>
                </div>
            )}
        </div>
    );
}