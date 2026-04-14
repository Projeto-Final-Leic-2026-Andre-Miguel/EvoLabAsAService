import type React from "react";
import { useAuth } from "../contexts/AuthContext";
import { useNavigate } from "react-router-dom";

export function ProtectedRoute({ children }: { children: React.ReactNode }) {

const { user, isLoading } = useAuth();
    
const navigate = useNavigate();

if (isLoading) {
    return <div style={{ textAlign: "center", padding: "2rem" }}>Loading...</div>;
}

if (!user) {
    navigate("/login");
    return null;
}

return <>{children}</>;
}