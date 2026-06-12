import React, { useEffect, useReducer, useState } from "react";
import { motion } from "framer-motion";
import styles from "./Credentials.module.css";
import { apiCredentials } from "./apiCredentials";
import type { LLM, LLMCredentials } from "../../types/credentials";
import { useAuth } from "../../contexts/AuthContext";
import { useValidCredentials } from "../../contexts/ValidCredentialsContext";
import { getErrorMessage } from "../../utils/errorsDescriptions";
import { Alert } from "../../components/ui/Alert";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { LoadingSpinner } from "../../components/ui/LoadingSpinner";
import { Modal } from "../../components/ui/Modal";
import { getCredentialLabel } from "../../utils/credentialLabels";
import { useToast } from "../../hooks/useToast";
import { usePageTitle } from "../../hooks/usePageTitle";
import { apiConfigs } from "../configs/apiConfigs";

const PROVIDER_INFO: Record<LLM, { icon: string, bgClass: string, label: string }> = {
    OPENAI: { icon: "O", bgClass: styles.bgOpenAI, label: "OpenAI" },
    GEMINI: { icon: "G", bgClass: styles.bgGemini, label: "Google Gemini" },
    ANTHROPIC: { icon: "A", bgClass: styles.bgAnthropic, label: "Anthropic Claude" },
    LOCAL_MODEL: { icon: "L", bgClass: styles.bgOllama, label: "Local Model" },
};

type State = {
    credentials: LLMCredentials[];
    isLoading: boolean;
    isModalOpen: boolean;
    editId: number | null;
    provider: LLM;
    apiKey: string;
    port: string;
    modelName: string;
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
    | { type: "SET_PORT"; payload: string }
    | { type: "SET_MODEL_NAME"; payload: string }
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
    port: "11434",
    modelName: "",
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
            return { ...state, isModalOpen: true, provider: "OPENAI", apiKey: "", port: "11434", modelName: "", editId: null, errorMessage: null };
        case "OPEN_MODAL_EDIT":
            return { ...state, isModalOpen: true, provider: action.payload.llm, apiKey: "", port: "11434", modelName: "", editId: action.payload.id, errorMessage: null };
        case "CLOSE_MODAL":
            return { ...state, isModalOpen: false, errorMessage: null };
        case "SET_PROVIDER":
            return { ...state, provider: action.payload };
        case "SET_API_KEY":
            return { ...state, apiKey: action.payload };
        case "SET_PORT":
            return { ...state, port: action.payload };
        case "SET_MODEL_NAME":
            return { ...state, modelName: action.payload };
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
    usePageTitle("Credentials");
    const { user } = useAuth();
    const { validCredentialsMap, validateCredential, setCredentialValidity } = useValidCredentials();
    const { showSuccess } = useToast();

    const [state, dispatch] = useReducer(reducer, initialState);
    const [credentialToDelete, setCredentialToDelete] = useState<LLMCredentials | null>(null);
    const [deletingId, setDeletingId] = useState<number | null>(null);
    const [credentialUsage, setCredentialUsage] = useState<Record<number, number[]>>({});

    async function fetchCredentials() {
        dispatch({ type: "FETCH_START" });
        const [credentialsResult, configsResult] = await Promise.all([
            apiCredentials.getAll(),
            apiConfigs.getAllMyConfigs(),
        ]);

        if (configsResult.type === "Success") {
            const usage = configsResult.data.reduce<Record<number, number[]>>((acc, config) => {
                (acc[config.llmCredentialsId] ??= []).push(config.configId);
                return acc;
            }, {});
            setCredentialUsage(usage);
        } else {
            setCredentialUsage({});
        }

        if (credentialsResult.type === "Success") {
            credentialsResult.data.forEach(c => {
                if (validCredentialsMap[c.id] === undefined) {
                    setCredentialValidity(c.id, true);
                }
            });
            dispatch({ type: "FETCH_SUCCESS", payload: credentialsResult.data });
        } else {
            dispatch({ type: "FETCH_SUCCESS", payload: [] });
            dispatch({ type: "SET_ERROR", payload: getErrorMessage(credentialsResult.error) });
        }
    }

    useEffect(() => {
        if (user) fetchCredentials();
    }, [user]); // eslint-disable-line react-hooks/exhaustive-deps

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        dispatch({ type: "SUBMIT_START" });

        if (state.editId) {
            const res = await apiCredentials.update(state.editId, { llm: state.provider, apiKey: state.apiKey });
            if (res.type === "Success") {
                setCredentialValidity(res.data.id, true);
                dispatch({ type: "SUBMIT_SUCCESS_UPDATE", payload: res.data });
                showSuccess("Credential updated successfully.");
            } else {
                dispatch({ type: "SUBMIT_ERROR", payload: getErrorMessage(res.error) });
            }
        } else if (state.provider === "LOCAL_MODEL") {
            const portNumber = parseInt(state.port, 10);
            if (isNaN(portNumber)) {
                dispatch({ type: "SUBMIT_ERROR", payload: "The port must be a valid number." });
                return;
            }
            const res = await apiCredentials.createLocalModel({
                llm: "LOCAL_MODEL",
                apiKey: state.apiKey,
                port: portNumber,
                modelName: state.modelName,
            });
            if (res.type === "Success") {
                setCredentialValidity(res.data.id, true);
                dispatch({ type: "SUBMIT_SUCCESS_CREATE", payload: res.data });
                showSuccess("Local credential created successfully.");
            } else {
                dispatch({ type: "SUBMIT_ERROR", payload: getErrorMessage(res.error) });
            }
        } else {
            const res = await apiCredentials.create({ llm: state.provider, apiKey: state.apiKey });
            if (res.type === "Success") {
                setCredentialValidity(res.data.id, true);
                dispatch({ type: "SUBMIT_SUCCESS_CREATE", payload: res.data });
                showSuccess("Credential created successfully.");
            } else {
                dispatch({ type: "SUBMIT_ERROR", payload: getErrorMessage(res.error) });
            }
        }
    };

    const handleDelete = async (id: number) => {
        setDeletingId(id);
        const res = await apiCredentials.delete(id);
        if (res.type === "Success") {
            dispatch({ type: "DELETE_SUCCESS", payload: id });
            setCredentialToDelete(null);
            showSuccess("Credential deleted successfully.");
        } else {
            setCredentialToDelete(null);
            dispatch({ type: "SET_ERROR", payload: getErrorMessage(res.error) });
            setTimeout(() => dispatch({ type: "SET_ERROR", payload: null }), 5000);
        }
        setDeletingId(null);
    };

    const handleValidate = async (id: number) => {
        dispatch({ type: "SET_VALIDATING", payload: id });
        const valid = await validateCredential(id);
        if (valid === null) {
            dispatch({ type: "SET_ERROR", payload: "Failed to validate the credential with the server." });
            setTimeout(() => dispatch({ type: "SET_ERROR", payload: null }), 5000);
        } else {
            showSuccess("Credential validation completed.");
        }
        dispatch({ type: "SET_VALIDATING", payload: null });
    };

    const renderCreatedAt = (createdAt?: string) => {
        if (!createdAt) return "Unknown date";
        const n = Number(createdAt);
        return !isNaN(n) ? new Date(n * 1000).toLocaleDateString() : new Date(createdAt).toLocaleDateString();
    };

    const editingCredential = state.editId
        ? state.credentials.find(credential => credential.id === state.editId) ?? null
        : null;

    return (
        <div className={styles.container}>
            <div className={styles.header}>
                <h1>API Credentials</h1>
                <button className={styles.createBtn} onClick={() => dispatch({ type: "OPEN_MODAL_CREATE" })}>
                    + New Credential
                </button>
            </div>
            
            {state.errorMessage && !state.isModalOpen && (
                <Alert variant="error">{state.errorMessage}</Alert>
            )}

            {state.isLoading ? (
                <LoadingSpinner label="Loading credentials" />
            ) : (
                <div className={styles.grid}>
                    {state.credentials.map(cred => {
                        const info = PROVIDER_INFO[cred.llm] || PROVIDER_INFO["OPENAI"];
                        const configIds = credentialUsage[cred.id] ?? [];
                        const deleteDisabled = configIds.length > 0;
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
                                    <h3 className={styles.providerName}>{getCredentialLabel(cred)}</h3>

                                    {state.validatingId === cred.id ? (
                                        <span className={`${styles.statusBadge} ${styles.statusNormal}`}>
                                            Validating...
                                        </span>
                                    ) : validCredentialsMap[cred.id] ? (
                                        <span
                                            className={`${styles.statusBadge} ${styles.statusValid}`}
                                            title="Valid credential"
                                            aria-label="Valid credential"
                                        >
                                            ✓
                                        </span>
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
                                    <button
                                        onClick={() => setCredentialToDelete(cred)}
                                        className={`${styles.actionBtn} ${styles.deleteBtn}`}
                                        disabled={deleteDisabled}
                                        title={deleteDisabled ? `In use by configuration #${configIds[0]} \u2014 delete it first.` : "Delete credential"}
                                    >
                                        Delete
                                    </button>
                                </div>
                            </motion.div>
                        );
                    })}

                    {state.credentials.length === 0 && (
                            <div className={styles.emptyState}>
                                No credentials found. Create one to get started.
                            </div>
                            )}

                </div>
            )}

            {state.isModalOpen && (
                <Modal
                    onClose={() => dispatch({ type: "CLOSE_MODAL" })}
                    ariaLabelledBy="credential-modal-title"
                    className={styles.modal}
                >
                        <div className={styles.modalHeader}>
                            <h2 id="credential-modal-title">
                                {editingCredential ? `Update ${getCredentialLabel(editingCredential)}` : "New Credential"}
                            </h2>
                            <button
                                type="button"
                                className={styles.closeBtn}
                                onClick={() => dispatch({ type: "CLOSE_MODAL" })}
                                aria-label="Close modal"
                            >
                                ×
                            </button>
                        </div>
                        {state.errorMessage && (
                            <Alert variant="error">{state.errorMessage}</Alert>
                        )}
                        <form onSubmit={handleSave}>
                            {state.editId ? (
                                <div className={styles.formGroup}>
                                    <label>API Key</label>
                                    <input
                                        type="password"
                                        required
                                        placeholder="Enter a new secret key"
                                        value={state.apiKey}
                                        onChange={e => dispatch({ type: "SET_API_KEY", payload: e.target.value })}
                                        className={styles.inputField}
                                    />
                                    <span className={styles.helperText}>Enter a new key to replace the current one.</span>
                                </div>
                            ) : (
                            <>
                            <div className={styles.formGroup}>
                                <label>Provider</label>
                                <select
                                    value={state.provider}
                                    onChange={e => dispatch({ type: "SET_PROVIDER", payload: e.target.value as LLM })}
                                    className={styles.inputField}
                                >
                                    <option value="OPENAI">OpenAI</option>
                                    <option value="GEMINI">Google Gemini</option>
                                    <option value="ANTHROPIC">Anthropic Claude</option>
                                    <option value="LOCAL_MODEL">Local Model (Ollama)</option>
                                </select>
                            </div>

                            {state.provider === "LOCAL_MODEL" ? (
                                <>
                                    <div className={styles.formGroup}>
                                        <label>Model Name</label>
                                        <input
                                            type="text"
                                            required
                                            placeholder="e.g. llama3, qwen2.5-coder:14b"
                                            value={state.modelName}
                                            onChange={e => dispatch({ type: "SET_MODEL_NAME", payload: e.target.value })}
                                            className={styles.inputField}
                                        />
                                    </div>
                                    <div className={styles.formGroup}>
                                        <label>Port</label>
                                        <input
                                            type="number"
                                            required
                                            placeholder="11434"
                                            value={state.port}
                                            onChange={e => dispatch({ type: "SET_PORT", payload: e.target.value })}
                                            className={styles.inputField}
                                        />
                                    </div>
                                    <div className={styles.formGroup}>
                                        <label>API Key <span className={styles.optionalLabel}>(optional)</span></label>
                                        <input
                                            type="password"
                                            placeholder="Leave Empty if not needed"
                                            value={state.apiKey}
                                            onChange={e => dispatch({ type: "SET_API_KEY", payload: e.target.value })}
                                            className={styles.inputField}
                                        />
                                    </div>
                                </>
                            ) : (
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
                            )}
                            </>
                            )}

                            <div className={styles.modalActions}>
                                <button type="button" onClick={() => dispatch({ type: "CLOSE_MODAL" })} className={styles.cancelBtn}>
                                    Cancel
                                </button>
                                <button type="submit" disabled={state.isSubmitting} className={styles.saveBtn}>
                                    {state.editId ? "Save Changes" : "Create"}
                                </button>
                            </div>
                        </form>
                </Modal>
            )}

            <ConfirmDialog
                isOpen={credentialToDelete !== null}
                title="Delete credential?"
                message={`Delete ${credentialToDelete ? getCredentialLabel(credentialToDelete) : "this credential"} permanently?`}
                isConfirming={deletingId !== null}
                onCancel={() => {
                    if (deletingId === null) setCredentialToDelete(null);
                }}
                onConfirm={() => {
                    if (credentialToDelete) handleDelete(credentialToDelete.id);
                }}
            />
        </div>
    );
}
