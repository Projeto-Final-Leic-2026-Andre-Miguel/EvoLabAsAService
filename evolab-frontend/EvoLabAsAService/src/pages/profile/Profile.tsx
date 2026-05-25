import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../contexts/AuthContext";
import styles from "./Profile.module.css";
import { motion } from "framer-motion";
import { request } from "../../api/api";
import { apiUsers } from "../auth/data/apiUsers";
import { clearAuthCookies } from "../../utils/authCookies";

interface UserStatistics {
    userId: number;
    projectsCreated: number;
    projectsExecuted: number;
    projectsSucceeded: number;
    projectsFailed: number;
    credentialsCreated: number;
    configsCreated: number;
    lastProjectId: number | null;
    lastProjectName: string | null;
    lastProjectCreatedAt: string | null;
    updatedAt: string;
}

const emptyStats: UserStatistics = {
    userId: 0,
    projectsCreated: 0,
    projectsExecuted: 0,
    projectsSucceeded: 0,
    projectsFailed: 0,
    credentialsCreated: 0,
    configsCreated: 0,
    lastProjectId: null,
    lastProjectName: null,
    lastProjectCreatedAt: null,
    updatedAt: "",
};

export function Profile() {
    const { user, isLoading, reload } = useAuth();
    const navigate = useNavigate();
    const [statistics, setStatistics] = useState<UserStatistics | null>(null);
    const [isDeleting, setIsDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState<string | null>(null);

    useEffect(() => {
        let active = true;
        if (!user) return;

        request<UserStatistics>("/api/statistics/me", { method: "GET" }).then(result => {
            if (!active) return;
            setStatistics(result.type === "Success" && result.data ? result.data : emptyStats);
        });

        return () => {
            active = false;
        };
    }, [user]);

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

    const handleDeleteAccount = async () => {
        const confirmed = window.confirm("Delete your account permanently? This removes your projects, credentials and configurations.");
        if (!confirmed) return;

        setIsDeleting(true);
        setDeleteError(null);
        const result = await apiUsers.deleteUser(user.id);
        if (result.type === "Success") {
            clearAuthCookies();
            reload();
            navigate("/");
        } else {
            setDeleteError(result.error?.message || "Could not delete account.");
            setIsDeleting(false);
        }
    };

    const stats = statistics ?? emptyStats;
    const successRatio = stats.projectsExecuted > 0
        ? Math.round((stats.projectsSucceeded / stats.projectsExecuted) * 100)
        : 0;
    const maxBarValue = Math.max(
        stats.projectsCreated,
        stats.projectsExecuted,
        stats.projectsSucceeded,
        stats.projectsFailed,
        stats.credentialsCreated,
        stats.configsCreated,
        1,
    );
    const chartItems = [
        { label: "Projects", value: stats.projectsCreated, className: styles.barProjects },
        { label: "Executed", value: stats.projectsExecuted, className: styles.barExecuted },
        { label: "Success", value: stats.projectsSucceeded, className: styles.barSuccess },
        { label: "Failed", value: stats.projectsFailed, className: styles.barFailed },
        { label: "Credentials", value: stats.credentialsCreated, className: styles.barCredentials },
        { label: "Configs", value: stats.configsCreated, className: styles.barConfigs },
    ];

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

                    <div className={styles.statsPanel}>
                        <div className={styles.statsHeader}>
                            <div>
                                <h3>Statistics</h3>
                                <p>{successRatio}% success rate across executed projects</p>
                            </div>
                            <span className={styles.statPill}>{stats.projectsExecuted} runs</span>
                        </div>

                        <div className={styles.barChart}>
                            {chartItems.map(item => (
                                <div className={styles.barItem} key={item.label}>
                                    <div className={styles.barTrack}>
                                        <motion.div
                                            className={`${styles.barFill} ${item.className}`}
                                            initial={{ height: 0 }}
                                            animate={{ height: `${Math.max(6, (item.value / maxBarValue) * 100)}%` }}
                                            transition={{ duration: 0.5 }}
                                        />
                                    </div>
                                    <span className={styles.barValue}>{item.value}</span>
                                    <span className={styles.barLabel}>{item.label}</span>
                                </div>
                            ))}
                        </div>

                        <div className={styles.lastProject}>
                            <span>Last project</span>
                            <strong>{stats.lastProjectName ?? "No projects yet"}</strong>
                            {stats.lastProjectCreatedAt && (
                                <small>{renderCreatedAt(stats.lastProjectCreatedAt)}</small>
                            )}
                        </div>
                    </div>

                    <div className={styles.dangerZone}>
                        <div>
                            <h3>Delete Account</h3>
                            <p>This action permanently removes your account and data.</p>
                        </div>
                        <button className={styles.deleteAccountBtn} onClick={handleDeleteAccount} disabled={isDeleting}>
                            {isDeleting ? "Deleting..." : "Delete Account"}
                        </button>
                    </div>

                    {deleteError && <div className={styles.deleteError}>{deleteError}</div>}
                </div>
            </motion.div>
        </div>
    );
}
