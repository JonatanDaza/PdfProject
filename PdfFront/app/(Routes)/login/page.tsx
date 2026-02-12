"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

const LoginPage = () => {
    ///
    // <div className="flex items-center justify-center h-screen w-full">
        // <form className="min-w-md max-w-md min-h-auto mx-auto background-white p-8 rounded-lg shadow-xl">
        //     <div>
        //         <h1 className="text-2xl font-bold text-heading mb-5">Iniciar sesión</h1>
        //     </div>
        //     <div className="mb-5">
        //         <label form="email" className="block mb-2.5 text-sm font-medium text-heading">correo</label>
        //         <input type="email" id="email" className="bg-neutral-secondary-medium border border-default-medium text-heading text-sm rounded-base focus:ring-brand focus:border-brand block w-full px-3 py-2.5 shadow-xs placeholder:text-body" placeholder="@zonamedica.com" required />
        //     </div>
        //     <div className="mb-5">
        //         <label form="password" className="block mb-2.5 text-sm font-medium text-heading">contraseña</label>
        //         <input type="password" id="password" className="bg-neutral-secondary-medium border border-default-medium text-heading text-sm rounded-base focus:ring-brand focus:border-brand block w-full px-3 py-2.5 shadow-xs placeholder:text-body" placeholder="••••••••" required />
        //     </div>
        //     {/* <label form="remember" className="flex items-center mb-5">
        //         <input id="remember" type="checkbox" value="" className="w-4 h-4 border border-default-medium rounded-xs bg-neutral-secondary-medium focus:ring-2 focus:ring-brand-soft" required />
        //         <p className="ms-2 text-sm font-medium text-heading select-none">I agree with the <a href="#" className="text-fg-brand hover:underline">terms and conditions</a>.</p>
        //     </label> */}
        //     <div className="flex items-center justify-between">
        //         <div className="pt-12 rounded-lg shadow-md flex flex-col-gap-4">
        //             <button type="submit" className="text-black bg-brand box-border border border-transparent hover:bg-brand-strong focus:ring-4 focus:ring-brand-medium shadow-xs font-medium leading-5 rounded-base text-sm px-4 py-2.5 focus:outline-none">Acceder</button>
        //         </div>
        //     </div>
        // </form>
    // </div>
    //
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const router = useRouter();

  const handleSubmit = async (e: any) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    setIsLoading(true);

    try {
      // Usar authService centralizado para login
    //   const userData = await authService.login(email, password);
      
      setSuccess("Inicio de sesión exitoso");
      
      // Redirigir según el rol del usuario
    //   const redirectUrl = authService.getRedirectUrl();
    //   setTimeout(() => {
    //     router.push(redirectUrl);
    //   }, 500);
      
    } catch (err: any) {
      setError(err.message || "Error de red o servidor");
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
            <div className=" rounded-full p-3 mb-4 shadow-lg border">
              {/* <img 
                src="/images/logo.png" 
                alt="" 
                className="h-16 w-16"
              /> */}
            </div>
            <h1 className="text-4xl font-bold mb-2 text-black">SLHC</h1>
            {/* <div className="w-16 h-1 bg-gradient-to-r from-transparent via-[#61dafb] to-transparent mb-2"></div> */}
            <p className="text-gray-400 text-sm">Inicia sesión en tu cuenta</p>
          </div>
          
          <form className="space-y-5">
            <div>
              <label htmlFor="email" className="block mb-2 font-semibold text-gray-500">Correo electrónico</label>
              <input
                type="email"
                name="email"
                id="email"
                required
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full px-4 py-3 rounded-lg bg-[#444] text-white border border-[#555] focus:outline-none focus:ring-2 focus:ring-[#61dafb] focus:border-transparent transition-all"
                placeholder="tu@email.com"
              />
            </div>
            <div>
              <label htmlFor="password" className="block mb-2 font-semibold text-gray-500">Contraseña</label>
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  name="password"
                  id="password"
                  required
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  className="w-full px-4 py-3 rounded-lg bg-[#444] text-white border border-[#555] focus:outline-none focus:ring-2 focus:ring-[#61dafb] focus:border-transparent transition-all pr-12"
                  placeholder="Ingresa tu contraseña"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-black transition-colors"
                >
                  {showPassword ? (
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88" />
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                      <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z" />
                      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  )}
                </button>
              </div>
            </div>
            {error && <div className="text-red-400 text-sm text-center bg-red-900/20 py-2 px-3 rounded-lg border border-red-700/50">{error}</div>}
            {success && <div className="text-green-400 text-sm text-center bg-green-900/20 py-2 px-3 rounded-lg border border-green-700/50">{success}</div>}
            
            <div className="text-right text-sm mb-4">
              <a href="/forgot-password" className="text-black hover:text-white transition-colors">
                {/* ¿Olvidaste tu contraseña? */}
              </a>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full text-black hover:text-blue-800 py-3 rounded-lg hover:from-[#06174d] hover:to-black transition-all font-bold shadow-lg transform hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? 'Iniciando sesión...' : 'Iniciar sesión'}
            </button>
            
            <div className="text-center text-sm mt-4">
              <span className="text-gray-400">¿No tienes una cuenta?</span>{" "}
              <a href="/register" className="text-blue-600 hover:text-blue-900 font-semibold transition-colors">
                Regístrate aquí
              </a>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}

export default LoginPage;