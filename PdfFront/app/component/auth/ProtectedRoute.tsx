"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import useAuth from "@/hooks/useAuth";

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredRole?: string;
  fallback?: React.ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requiredRole, 
  fallback 
}) => {
  const router = useRouter();
  const { checkAuth, hasRole, isLoading } = useAuth();
  const [isChecking, setIsChecking] = useState(true);
  const [hasAccess, setHasAccess] = useState(false);

  useEffect(() => {
    const verifyAuth = async () => {
      try {
        console.log("[ProtectedRoute] Verificando autenticacion...");
        const authenticated = await checkAuth();
        
        console.log("[ProtectedRoute] Autenticado?", authenticated);
        console.log("[ProtectedRoute] Rol requerido?", requiredRole);
        
        if (!authenticated) {
          console.log("[ProtectedRoute] Redirigiendo a login (no autenticado)");
          setIsChecking(false);
          router.push('/login');
          return;
        }

        // Si se requiere un rol especifico, verificar
        if (requiredRole) {
          const hasRequiredRole = hasRole(requiredRole);
          console.log("[ProtectedRoute] Tiene rol requerido?", hasRequiredRole);
          
          if (!hasRequiredRole) {
            setHasAccess(false);
            setIsChecking(false);
            return;
          }
        }

        // Usuario esta autenticado (y tiene el rol si era requerido)
        console.log("[ProtectedRoute] Acceso permitido");
        setHasAccess(true);
        setIsChecking(false);
      } catch (error) {
        console.error("[ProtectedRoute] Error verificando autenticacion:", error);
        setIsChecking(false);
        router.push('/login');
      }
    };

    verifyAuth();
  }, [checkAuth, hasRole, requiredRole, router]);

  // Loader mientras verifica
  if (isChecking || isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-blue-500 mx-auto mb-4"></div>
          <p className="text-gray-600">Verificando acceso...</p>
        </div>
      </div>
    );
  }

  // No tiene acceso
  if (!hasAccess) {
    if (fallback) {
      return <>{fallback}</>;
    }

    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        {/* <div className="text-center max-w-md">
          <div className="bg-red-100 border border-red-400 text-red-700 px-6 py-4 rounded-lg">
            <h2 className="text-2xl font-bold mb-2">🚫 Acceso Denegado</h2>
            <p className="mb-4">No tienes los permisos necesarios para acceder.</p>
            {requiredRole && (
              <p className="text-sm mb-4">
                <strong>Rol requerido:</strong> {requiredRole}
              </p>
            )}
            <button
              onClick={() => router.push("/dashboard")}
              className="bg-blue-500 text-white px-4 py-2 rounded hover:bg-blue-600 transition-colors"
            >
              Volver al Dashboard
            </button>
          </div>
        </div> */}
      </div>
    );
  }

  // Usuario autenticado y con acceso - mostrar contenido
  return <>{children}</>;
};

export default ProtectedRoute;