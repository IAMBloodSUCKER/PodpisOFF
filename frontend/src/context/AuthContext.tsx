import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';
import { api, setUnauthorizedHandler } from '../api/api';
import { useI18n } from './I18nContext';
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
  isReady: boolean;
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

function persistSession(session: AuthSession) {
  if (localStorage.getItem(STORAGE_KEY)) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  }
  if (sessionStorage.getItem(SESSION_KEY)) {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const { setLocale } = useI18n();
  const [session, setSession] = useState<AuthSession | null>(readSession);
  const [isReady, setIsReady] = useState(() => !readSession()?.token);
  const validatedTokenRef = useRef<string | null>(null);

  const logout = useCallback(() => {
    clearSession();
    validatedTokenRef.current = null;
    setSession(null);
    setIsReady(true);
  }, []);

  const login = useCallback((nextSession: AuthSession, rememberMe: boolean) => {
    saveSession(nextSession, rememberMe);
    validatedTokenRef.current = nextSession.token;
    setSession(nextSession);
    setIsReady(true);
  }, []);

  const updateUser = useCallback((user: AuthResponse) => {
    setSession((prev) => {
      if (!prev) return prev;
      const nextSession: AuthSession = { ...prev, user };
      persistSession(nextSession);
      return nextSession;
    });
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(logout);
    return () => setUnauthorizedHandler(() => {});
  }, [logout]);

  useEffect(() => {
    const token = session?.token;
    if (!token) {
      validatedTokenRef.current = null;
      setIsReady(true);
      return;
    }

    if (validatedTokenRef.current === token) {
      setIsReady(true);
      return;
    }

    let cancelled = false;
    setIsReady(false);

    api
      .me()
      .then((user) => {
        if (cancelled) return;
        updateUser(user);
        setLocale(user.locale);
        validatedTokenRef.current = token;
      })
      .catch(() => {
        if (cancelled) return;
        logout();
      })
      .finally(() => {
        if (!cancelled) setIsReady(true);
      });

    return () => {
      cancelled = true;
    };
  }, [session?.token, logout, setLocale, updateUser]);

  const value = useMemo<AuthState>(
    () => ({
      token: session?.token ?? null,
      user: session?.user ?? null,
      isAuthenticated: Boolean(session?.token),
      isReady,
      login,
      logout,
      updateUser,
    }),
    [session, isReady, login, logout, updateUser],
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
