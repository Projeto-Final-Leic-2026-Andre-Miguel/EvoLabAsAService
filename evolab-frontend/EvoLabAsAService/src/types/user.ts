export enum AuthProvider {
    LOCAL = 'LOCAL',
    GOOGLE = 'GOOGLE',
}
export interface User {
    id: number;
    name: string;
    email: string;
    passwordHash: string | null;
    authProvider: AuthProvider;
    providerId: string | null;
    createdAt: string; // ISO 8601 Instant
}
export interface AuthenticatedUser {
    user: User;
    token: string;
}
export interface TokenExternalInfo {
    tokenValue: string;
    tokenExpiration: string; // ISO 8601 Instant
}
export interface CreateLocalUserInput {
    name: string;
    email: string;
    password?: string;
}
export interface CreateOAuthUserInput {
    name: string;
    email: string;
    provider: AuthProvider;
    providerId: string;
}
export interface CreateTokenInput {
    email: string;
    password?: string;
}
