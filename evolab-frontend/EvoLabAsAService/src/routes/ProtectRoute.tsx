import type React from "react";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate} from "react-router-dom";
import styles from "./ProtectRoute.module.css";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {

const { user, isLoading } = useAuth();
const navigate = useNavigate()

if (isLoading) {
    return (
        <div className={styles.stateShell}>
            <section className={styles.panel} aria-live="polite">
                <div className={styles.statusMark}>
                    <span className={styles.spinner} />
                </div>
                <h2 className={styles.title}>Checking session</h2>
                <p className={styles.message}>Validating your secure session before opening the workspace.</p>
            </section>
        </div>
    );
}

if (!user) {
    return (
        <div className={styles.stateShell}>
            <section className={styles.panel}>
                <div className={styles.statusMark}>!</div>
                <h2 className={styles.title}>Authentication required</h2>
                <p className={styles.message}>Sign in to access your projects, runs, credentials, and experiment history.</p>
                <button className={styles.button} onClick={() => navigate("/login") }>Go to login</button>
            </section>
        </div>
    );
}

return <>{children}</>;
}
