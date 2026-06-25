"use client";

import { useCallback, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth/authService";

interface User {
  id?: string;
  name?: string;
  email?: string;
  role?: string;
  roles?: Array<string | { name: string }>;
  [key: string]: any;
}

interface UseAuthReturn {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: User | null;
  token: string | null;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  checkAuth: () => Promise<boolean>;
  initializeAuth: () => Promise<void>;
  clearError: () => void;
  hasRole: (roleName: string) => boolean;
  hasAnyRole: (roleNames: string[]) => boolean;
  getRoles: () => string[];
  getHighestPriorityRole: () => string | undefined;
}

// Prioridad de roles — debe coincidir con el middleware
const ROLE_PRIORITY = ['ADMIN', 'MEDIC', 'COLLABORATOR', 'USER'];

function normalizeRoles(user: User | null): string[] {
  if (!user) return [];

  const raw: Array<string | { name: string }> = [];

  if (user.role) raw.push(user.role);
  if (Array.isArray(user.roles)) raw.push(...user.roles);

  return [...new Set(
    raw.map((r) =>
      (typeof r === 'object' && r !== null ? r.name : String(r)).toUpperCase()
    )
  )];
}

function getRedirectForRoles(roles: string[]): string {
  const top = ROLE_PRIORITY.find((r) => roles.includes(r));
  return top === 'ADMIN' ? '/dashboard' : '/inicio';
}

export const useAuth = (): UseAuthReturn => {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setErrorState] = useState<string | null>(null);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('userData');

    if (storedToken) setToken(storedToken);

    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch (e) {
        console.error("[useAuth] Error parseando userData:", e);
      }
    }

    setIsLoading(false);
  }, []);

  const getRoles = useCallback((): string[] => {
    return normalizeRoles(user);
  }, [user]);

  const hasRole = useCallback((roleName: string): boolean => {
    return normalizeRoles(user).includes(roleName.toUpperCase());
  }, [user]);

  const hasAnyRole = useCallback((roleNames: string[]): boolean => {
    const roles = normalizeRoles(user);
    return roleNames.some((r) => roles.includes(r.toUpperCase()));
  }, [user]);

  const getHighestPriorityRole = useCallback((): string | undefined => {
    const roles = normalizeRoles(user);
    return ROLE_PRIORITY.find((r) => roles.includes(r));
  }, [user]);

  const checkAuth = useCallback(async (): Promise<boolean> => {
    if (typeof window === 'undefined') return false;
    const storedToken = localStorage.getItem('token');
    const storedUserData = localStorage.getItem('userData');
    return !!(storedToken && storedUserData);
  }, []);

  const initializeAuth = useCallback(async (): Promise<void> => {
    try {
      const storedToken = localStorage.getItem('token');
      if (!storedToken) router.push('/login');
    } catch {
      router.push('/login');
    }
  }, [router]);

  const login = useCallback(async (email: string, password: string): Promise<void> => {
    try {
      setIsLoading(true);
      await authService.login(email, password);

      const storedToken = localStorage.getItem('token');
      const storedUserData = localStorage.getItem('userData');

      if (storedToken) setToken(storedToken);

      let parsedUser: User | null = null;
      if (storedUserData) {
        try {
          parsedUser = JSON.parse(storedUserData);
          setUser(parsedUser);
        } catch (e) {
          console.error("[useAuth] Error parseando userData tras login:", e);
        }
      }

      // Usa todos los roles para determinar el redirect
      const roles = normalizeRoles(parsedUser);
      router.push(getRedirectForRoles(roles));
    } catch (err: any) {
      const message = err.message || "Error en la autenticación";
      setErrorState(message);
      throw new Error(message);
    } finally {
      setIsLoading(false);
    }
  }, [router]);

  const logout = useCallback(async (): Promise<void> => {
    try {
      authService.logout();
    } catch (e) {
      console.error("[useAuth] Error en logout:", e);
    } finally {
      setToken(null);
      setUser(null);
      setErrorState(null);
      router.push('/login');
    }
  }, [router]);

  const clearError = useCallback(() => setErrorState(null), []);

  return {
    isAuthenticated: !!token,
    isLoading,
    user,
    token,
    error,
    login,
    logout,
    checkAuth,
    initializeAuth,
    clearError,
    hasRole,
    hasAnyRole,
    getRoles,
    getHighestPriorityRole,
  };
};

export default useAuth;