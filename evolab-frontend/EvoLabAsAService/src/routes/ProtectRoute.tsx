import type React from "react";
import { useAuth } from "../contexts/AuthContext";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {

const { user, isLoading } = useAuth();

if (isLoading) {
    return <div style={{ textAlign: "center", padding: "2rem" }}>Loading...</div>;
}

if (!user) {
    return (
        <div style={{ textAlign: "center", padding: "4rem" }}>
            <h2>Authentication Required</h2>
            <p>You need to be logged in to access this page.</p>
            <button onClick={() => window.location.href = "/login"}>Go to Login</button>
        </div>
    );
}

return <>{children}</>;
}
