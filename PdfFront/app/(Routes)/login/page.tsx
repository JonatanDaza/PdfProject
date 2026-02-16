"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { authService } from "@/services/auth/authService";

const LoginPage = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const router = useRouter();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isLoading) return;
    
    setError("");
    setIsLoading(true);

    try {
      const response = await authService.login(email, password);
      
      // Guardar datos del usuario
      if (response?.data) {
        localStorage.setItem('userData', JSON.stringify({
          id: response.data.id,
          email: response.data.email,
          name: response.data.name,
          role: response.data.roles[0],
          token: response.data.token
        }));
        
        // Redirigir según rol
        router.push(
          response.data.roles.includes('ADMIN', 'USER') 
            ? '/dashboard'
            : '/dashboard'
        );
      }
    } catch (err: any) {
      setError(err.message || "Error al iniciar sesión");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col min-h-screen">
      <main className="flex-1 font-poppins text-white flex flex-col items-center justify-center p-4">
        <div className="border rounded-xl shadow-2xl p-10 w-full max-w-lg">
          {/* Logo y título */}
          <div className="flex flex-col items-center mb-8">
            <h1 className="text-4xl font-bold mb-2 text-black">SLHC</h1>
            <p className="text-gray-400 text-sm">Inicia sesión en tu cuenta</p>
          </div>
          
          <form className="space-y-5" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="email" className="block mb-2 font-semibold text-gray-500">
                Correo electrónico
              </label>
              <input
                type="email"
                id="email"
                required
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full px-4 py-3 rounded-lg bg-[#444] text-white border border-[#555] focus:outline-none focus:ring-2 focus:ring-[#61dafb]"
                placeholder="tu@email.com"
                disabled={isLoading}
              />
            </div>
            
            <div>
              <label htmlFor="password" className="block mb-2 font-semibold text-gray-500">
                Contraseña
              </label>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  id="password"
                  required
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  className="w-full px-4 py-3 rounded-lg bg-[#444] text-white border border-[#555] focus:outline-none focus:ring-2 focus:ring-[#61dafb] pr-12"
                  placeholder="Ingresa tu contraseña"
                  disabled={isLoading}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-black"
                >
                  {showPassword ? "👁️" : "👁️"}
                </button>
              </div>
            </div>

            {error && (
              <div className="text-red-400 text-sm text-center bg-red-900/20 py-2 px-3 rounded-lg">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={isLoading}
              className="w-full text-black hover:text-blue-800 py-3 rounded-lg transition-all disabled:opacity-50"
            >
              {isLoading ? 'Iniciando sesión...' : 'Iniciar sesión'}
            </button>
            
            <div className="text-center text-sm mt-4">
              <span className="text-gray-400">¿No tienes una cuenta?</span>{" "}
              <a href="/register" className="text-blue-600 hover:text-blue-900 font-semibold">
                Regístrate aquí
              </a>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
};

export default LoginPage;