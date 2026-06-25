"use client";

import React, { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import useAuth from "@/hooks/useAuth";

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRoles?: string | string[];
  fallback?: React.ReactNode;
}

const Loader = () => (
  <div className="flex items-center justify-center min-h-screen bg-gray-50">
    <div className="text-center">
      <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mx-auto mb-4" />
      <p className="text-gray-600">Verificando acceso...</p>
    </div>
  </div>
);

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredRoles,
  fallback,
}) => {
  const router = useRouter();
  const { checkAuth, hasAnyRole, getHighestPriorityRole, isLoading } = useAuth();
  const [isChecking, setIsChecking] = useState(true);
  const [hasAccess, setHasAccess] = useState(false);

  useEffect(() => {
    if (isLoading) return;

    const verifyAuth = async () => {
      try {
        const authenticated = await checkAuth();

        if (!authenticated) {
          router.replace('/login');
          return;
        }

        if (requiredRoles) {
          const roles = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles];
          const allowed = hasAnyRole(roles);

          if (!allowed) {
            // Redirect basado en el rol de mayor prioridad del usuario
            const topRole = getHighestPriorityRole();
            const fallbackRoute = topRole === 'ADMIN' ? '/dashboard' : '/inicio';
            router.replace(fallbackRoute);
            return;
          }
        }

        setHasAccess(true);
      } catch (error) {
        console.error("[ProtectedRoute] Error verificando autenticación:", error);
        router.replace('/login');
      } finally {
        setIsChecking(false);
      }
    };

    verifyAuth();
  }, [isLoading, checkAuth, hasAnyRole, getHighestPriorityRole, requiredRoles, router]);

  if (isChecking || isLoading) return <Loader />;
  if (!hasAccess) return fallback ? <>{fallback}</> : null;

  return <>{children}</>;
};

export default ProtectedRoute;