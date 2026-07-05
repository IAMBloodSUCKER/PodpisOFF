import { createContext, useContext, useMemo, useState } from 'react';
import { AuthResponse } from '../types/api';

const STORAGE_KEY = 'podpisoff.auth';
const SESSION_KEY = 'podpisoff.auth.session';

interface AuthSession {
  token: string;
  user: AuthResponse;
}

interface AuthState {
  token: string | null;
  user: AuthResponse | null;
  isAuthenticated: boolean;
  login: (session: AuthSession, rememberMe: boolean) => void;
  logout: () => void;
  updateUser: (user: AuthResponse) => void;
}

const AuthContext = createContext<AuthState | null>(null);

function readSession(): AuthSession | null {
  const localRaw = localStorage.getItem(STORAGE_KEY);
  const sessionRaw = sessionStorage.getItem(SESSION_KEY);
  const source = localRaw ?? sessionRaw;
  if (!source) return null;
  try {
    return JSON.parse(source) as AuthSession;
  } catch {
    return null;
  }
}

function saveSession(session: AuthSession, rememberMe: boolean) {
  const payload = JSON.stringify(session);
  if (rememberMe) {
    localStorage.setItem(STORAGE_KEY, payload);
    sessionStorage.removeItem(SESSION_KEY);
  } else {
    sessionStorage.setItem(SESSION_KEY, payload);
    localStorage.removeItem(STORAGE_KEY);
  }
}

function clearSession() {
  localStorage.removeItem(STORAGE_KEY);
  sessionStorage.removeItem(SESSION_KEY);
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(readSession);

  const value = useMemo<AuthState>(
    () => ({
      token: session?.token ?? null,
      user: session?.user ?? null,
      isAuthenticated: Boolean(session?.token),
      login: (nextSession, rememberMe) => {
        saveSession(nextSession, rememberMe);
        setSession(nextSession);
      },
      logout: () => {
        clearSession();
        setSession(null);
      },
      updateUser: (user) => {
        if (!session) return;
        const nextSession: AuthSession = { ...session, user };
        setSession(nextSession);
        if (localStorage.getItem(STORAGE_KEY)) {
          localStorage.setItem(STORAGE_KEY, JSON.stringify(nextSession));
        }
        if (sessionStorage.getItem(SESSION_KEY)) {
          sessionStorage.setItem(SESSION_KEY, JSON.stringify(nextSession));
        }
      },
    }),
    [session],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return ctx;
}
