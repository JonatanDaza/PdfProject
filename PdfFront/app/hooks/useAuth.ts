"use client";

import { useCallback, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { authService } from "@/services/auth/authService";

interface User {
  id?: string;
  name?: string;
  email?: string;
  role?: string;
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
}

export const useAuth = (): UseAuthReturn => {
  const router = useRouter();
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setErrorState] = useState<string | null>(null);

  // Cargar datos iniciales
  useEffect(() => {
    if (typeof window === 'undefined') return;

    console.log("[useAuth] Cargando datos del localStorage...");
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('userData');

    console.log("[useAuth] Token encontrado:", !!storedToken);
    console.log("[useAuth] User data:", storedUser);

    if (storedToken) {
      setToken(storedToken);
    }

    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser);
        console.log("[useAuth] Usuario parseado:", parsedUser);
        setUser(parsedUser);
      } catch (e) {
        console.error("[useAuth] Error parseando usuario:", e);
      }
    }

    setIsLoading(false);
  }, []);

  const checkAuth = useCallback(async (): Promise<boolean> => {
    try {
      if (typeof window === 'undefined') return false;
      
      const token = localStorage.getItem('token');
      const userData = localStorage.getItem('userData');
      
      console.log("[useAuth] checkAuth - Token:", !!token);
      console.log("[useAuth] checkAuth - UserData:", !!userData);
      console.log("[useAuth] checkAuth - Token valor:", token ? token.substring(0, 20) + '...' : 'null');
      
      const hasAuth = !!(token && userData);
      console.log("[useAuth] checkAuth resultado:", hasAuth);
      
      return hasAuth;
    } catch (error) {
      console.error("[useAuth] checkAuth error:", error);
      return false;
    }
  }, []);

  const initializeAuth = useCallback(async (): Promise<void> => {
    try {
      const token = localStorage.getItem('token');
      
      if (token) {
        console.log("✅ [useAuth] Sesión encontrada");
        return;
      }

      console.log("❌ [useAuth] No hay token, redirigiendo a login");
      router.push('/login');
    } catch (error) {
      console.error("❌ [useAuth] Error en initializeAuth:", error);
      router.push('/login');
    }
  }, [router]);

  const login = useCallback(async (email: string, password: string): Promise<void> => {
    try {
      setIsLoading(true);
      const data = await authService.login(email, password);
      
      console.log("[useAuth] Login exitoso, datos:", data);
      
      // Actualizar estado local desde localStorage (ya fue guardado en authService)
      const storedToken = localStorage.getItem('token');
      const storedUserData = localStorage.getItem('userData');
      
      console.log("[useAuth] Token guardado en localStorage:", !!storedToken);
      console.log("[useAuth] UserData guardado en localStorage:", !!storedUserData);
      
      if (storedToken) {
        setToken(storedToken);
      }
      
      if (storedUserData) {
        try {
          const parsedUser = JSON.parse(storedUserData);
          setUser(parsedUser);
        } catch (e) {
          console.error("[useAuth] Error parseando userData después de login:", e);
        }
      }
      
      router.push('/dashboard');
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
      setToken(null);
      setUser(null);
      setErrorState(null);
      router.push('/login');
    } catch (error) {
      console.error("❌ [useAuth] Error en logout:", error);
      setToken(null);
      setUser(null);
      router.push('/login');
    }
  }, [router]);

  const clearError = useCallback(() => {
    setErrorState(null);
  }, []);

  const hasRole = useCallback((roleName: string): boolean => {
    if (!user) {
      console.log("[useAuth] hasRole - No hay usuario");
      return false;
    }

    // Buscar en diferentes posibles ubicaciones del rol
    const userRole = user.role || user.roles?.[0]?.name || user.roles?.[0];
    
    console.log("[useAuth] hasRole - Buscando rol:", roleName, "Usuario rol:", userRole);
    
    if (!userRole) return false;
    
    return String(userRole).toLowerCase() === roleName.toLowerCase();
  }, [user]);

  return {
    isAuthenticated: !!token,
    isLoading,
    user: user || null,
    token: token || null,
    error,
    login,
    logout,
    checkAuth,
    initializeAuth,
    clearError,
    hasRole,
  };
};

export default useAuth;