import React, { createContext, useContext, useEffect, useRef, useState } from "react";
import * as Routes from "@/components/routes";  

export type Session = Routes.Session;

const SessionContext = createContext<Session>({
    refreshToken: '',
    accessToken: '',
    expiration: 0,
    userId: 0,
});

const AuthContext = createContext<{
    session?: Session | null;
    login: (username: string, password: string) => Promise<void>;
    register: (username: string, password: string, biography: string, communityId: number) => Promise<{ message: string; userId: number }>;
    logout: () => void;
    refresh: () => Promise<void>;
    isTokenExpired: () => boolean;
    keepSession: () => Promise<boolean>;
    isLoading: boolean;
}>({
    session: null,
    login: async () => { },
    register: async () => ({ message: '', userId: 0 }),
    logout: () => { },
    refresh: async () => { },
    isTokenExpired: () => true,
    keepSession: async () => false,
    isLoading: false,
});

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [session, setSession] = useState<Session | null>(null);
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const keepAliveTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
    const isRefreshingRef = useRef<boolean>(false);

    const login = async (username: string, password: string) => {
        setIsLoading(true);
        try {
            const result = await Routes.login(username, password);
            if (!result.ok) {
                throw new Error(result.error);
            }
            setSession({
                accessToken: result.data.accessToken,
                refreshToken: result.data.refreshToken,
                expiration: result.data.expiresIn + Date.now() / 1000,
                userId: result.data.userId,
            });
        } catch (e) {
            throw new Error("Login failed: " + (e as Error).message);
        } finally {
            setIsLoading(false);
        }
    };

    const register = async (username: string, password: string, biography: string, communityId: number): Promise<{ message: string; userId: number }> => {
        setIsLoading(true);
        try {
            const result = await Routes.register(username, password, biography, communityId);
            if (!result.ok) {
                throw new Error(result.error);
            }
            return {
                message: result.data.message,
                userId: result.data.userId,
            };
        } catch (e) {
            throw new Error("Registration failed: " + (e as Error).message);
        } finally {
            setIsLoading(false);
        }
    };

    const logout = () => {
        setSession(null);
    }

    const isTokenExpired = () => {
        return !session?.refreshToken || session?.expiration <= Date.now() / 1000;
    }

    const refresh = async () => {
        setIsLoading(true);
        try {
            if(!session) {
                throw new Error("No session available");
            }

            if(isTokenExpired()) {
                throw new Error("No refresh token available");
            }

            const result = await Routes.refreshToken(session.userId, session.refreshToken);
            if (!result.ok) {
                throw new Error(result.error);
            }
            setSession({
                accessToken: result.data.accessToken,
                refreshToken: result.data.refreshToken,
                expiration: result.data.expiresIn + Date.now() / 1000,
                userId: session.userId,
            });
        } catch (e) {
            throw new Error("Failed to refresh token: " + (e as Error).message);
        } finally {
            setIsLoading(false);
        }
    }

    const keepSession = async () => {
        try {
            if (session && isTokenExpired() && !isRefreshingRef.current) {
                isRefreshingRef.current = true;
                await refresh();
            }
        } catch (e) {
            console.error("Failed to keep session alive:", e);
            logout();
            return false;
        } finally {
            isRefreshingRef.current = false;
        }
        return true;
    }
    useEffect(() => {
        let mounted = true;
        const bootstrap = async () => {
            setIsLoading(true);
            try {
                // Placeholder for loading persisted session if added later
                if (session) {
                    await keepSession();
                }
            } finally {
                if (mounted) setIsLoading(false);
            }
        };
        bootstrap();

        // Start interval to keep session alive every 60s
        if (!keepAliveTimerRef.current) {
            keepAliveTimerRef.current = setInterval(() => {
                keepSession();
            }, 60_000);
        }

        return () => {
            mounted = false;
            if (keepAliveTimerRef.current) {
                clearInterval(keepAliveTimerRef.current);
                keepAliveTimerRef.current = null;
            }
        };
        // It's intentional to not depend on session here to avoid re-creating interval
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Clear timers on logout
    useEffect(() => {
        if (!session && keepAliveTimerRef.current) {
            clearInterval(keepAliveTimerRef.current);
            keepAliveTimerRef.current = null;
        }
    }, [session]);
    return (
        <AuthContext.Provider value={{session, login, register, logout, refresh, isTokenExpired, keepSession, isLoading}}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    return useContext(AuthContext);
}

export function SessionProvider({ children }: { children: React.ReactNode }) {
    const { session, isLoading } = useAuth();
    const router = require('expo-router').router as import('expo-router').Router;

    useEffect(() => {
        if (!isLoading && !session) {
            router.replace('/(login)/login');
        }
    }, [isLoading, session]);

    if (isLoading || !session) {
        return null;
    }
    return (
        <SessionContext.Provider value = {session}>
            {children}
        </SessionContext.Provider>
    );
}

export function useSession() {
    return useContext(SessionContext);
}

