import { createContext, useContext, useState, type ReactNode } from "react";
import { apiCredentials } from "../pages/credentials/apiCredentials";

interface ValidCredentialsContextType {
    validCredentialsMap: Record<number, boolean>;
    validateCredential: (id: number) => Promise<boolean | null>;
    setCredentialValidity: (id: number, isValid: boolean) => void;
}

const ValidCredentialsContext = createContext<ValidCredentialsContextType | undefined>(undefined);

export function ValidCredentialsProvider({ children }: { children: ReactNode }) {
    const [validCredentialsMap, setValidCredentialsMap] = useState<Record<number, boolean>>({});

    const validateCredential = async (id: number): Promise<boolean | null> => {
        const res = await apiCredentials.validate(id);
        if (res.type === "Success") {
            const isValid = res.data.isValid;
            setValidCredentialsMap(prev => ({ ...prev, [id]: isValid }));
            return isValid;
        }
        return null;
    };

    const setCredentialValidity = (id: number, isValid: boolean) => {
        setValidCredentialsMap(prev => ({ ...prev, [id]: isValid }));
    };

    return (
        <ValidCredentialsContext.Provider value={{ validCredentialsMap, validateCredential, setCredentialValidity }}>
            {children}
        </ValidCredentialsContext.Provider>
    );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useValidCredentials() {
    const context = useContext(ValidCredentialsContext);
    if (!context) {
        throw new Error("useValidCredentials must be used within a ValidCredentialsProvider");
    }
    return context;
}

