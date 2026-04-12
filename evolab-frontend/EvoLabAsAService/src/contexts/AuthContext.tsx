import React, { createContext, useCallback, useContext, useState} from "react";
import {useFetch} from "../hooks/useFetch";
import {API_BASE_URL} from "../api/api";
import { type User } from "../types/user";

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  reload: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [version, setVersion] = useState(0);
  const userState = useFetch<User>(`${API_BASE_URL}/me`, undefined, version)

  const user = userState.type === "loaded" ? userState.payload : null;
  const isLoading = userState.type === "begin" || userState.type === "loading";

  const reload = useCallback(() => {
    setVersion(v => v + 1)
  },[]);
  
  return (
      <AuthContext.Provider value={{ user, isLoading, reload }}>
        {children}
      </AuthContext.Provider>
  );
}


export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}