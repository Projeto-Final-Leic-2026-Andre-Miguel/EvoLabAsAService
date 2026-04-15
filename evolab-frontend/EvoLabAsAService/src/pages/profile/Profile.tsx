import { useAuth } from "../../contexts/AuthContext";
import styles from "./Profile.module.css";
import { motion } from "framer-motion";

export function Profile() {
    const { user, isLoading } = useAuth();

    if (isLoading) {
        return <div className={styles.loadingContainer}>Loading Profile...</div>;
    }

    if (!user) {
        return <div className={styles.errorContainer}>No user logged in.</div>;
    }

    const initial = user.name ? user.name.charAt(0).toUpperCase() : "?";

    const renderCreatedAt = (createdAt?: string) => {
        if (!createdAt) return "Unknown date";
        const n = Number(createdAt);
        return !isNaN(n) ? new Date(n * 1000).toLocaleDateString() : new Date(createdAt).toLocaleDateString();
    };

    return (
        <div className={styles.profileContainer}>
            <motion.div
                className={styles.profileCard}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5, ease: "easeOut" }}
            >
                <div className={styles.cardHeader}>
                    <div className={styles.avatar}>{initial}</div>
                    <h2 className={styles.userName}>{user.name}</h2>
                    <p className={styles.userEmail}>{user.email}</p>
                    {user.authProvider && (
                        <span 
                            className={`${styles.badge} ${user.authProvider === "GOOGLE" ? styles.badgeGoogle : styles.badgeLocal}`}
                        >
                            {user.authProvider} ACCOUNT
                        </span>
                    )}
                </div>

                <div className={styles.cardBody}>
                    <div className={styles.infoRow}>
                        <span className={styles.infoLabel}>Member Since:</span>
                        <span className={styles.infoValue}>
                            {renderCreatedAt(user.createdAt)}
                        </span>
                    </div>

                    <div className={styles.statsPlaceholder}>
                        <p>Experimental algorithms and project statistics will appear here soon.</p>
                    </div>
                </div>
            </motion.div>
        </div>
    );
}