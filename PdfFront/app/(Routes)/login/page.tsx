"use client";

import { authService } from "@/services/auth/authService";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { 
  isFormValid, 
  loginValidations, 
  validateField
} from "@/component/auth/Formvalidation";
import Form from "@/component/UI/Form";

const LoginPage = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [touched, setTouched] = useState<Record<string, boolean>>({});
  const router = useRouter();

  const handleBlur = (field: string) =>
    setTouched((prev) => ({ ...prev, [field]: true }));

  const fieldError = (id: string, value: string) =>
    touched[id] ? validateField(id, value, loginValidations) : null;

  const canSubmit = isFormValid({ email, password }, loginValidations);

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
        
        // Redirigir según rol usando el servicio
        const redirectUrl = authService.getRedirectUrl();
        router.push(redirectUrl);
      }
    } catch (err: any) {
      setError(err.message || "Error al iniciar sesión");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Form
      img="/img/logo-zona-medica-header.png"
      title={null}
      subtitle="Inicia sesión en tu cuenta"
      onSubmit={handleSubmit}
      submitLabel="Iniciar sesión"
      isLoading={isLoading}
      loadingLabel="Iniciando sesión..."
      error={error}
      canSubmit={canSubmit}
      fields={[
        {
          id: "email",
          label: "Correo electrónico",
          type: "email",
          placeholder: "tu@zonamedicaips.com",
          required: true,
          value: email,
          onChange: setEmail,
          onBlur: () => handleBlur("email"),
          errorMessage: fieldError("email", email),
        },
        {
          id: "password",
          label: "Contraseña",
          type: showPassword ? "text" : "password",
          placeholder: "Ingresa tu contraseña",
          required: true,
          value: password,
          onChange: setPassword,
          onBlur: () => handleBlur("password"),
          errorMessage: fieldError("password", password),
          suffix: (
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="text-gray-400 hover:text-black"
            >
              👁️
            </button>
          ),
        },
      ]}
      footer={
        <>
          <span className="text-gray-400">¿No tienes una cuenta?</span>{" "}
          <a
            href="/register"
            className="text-blue-600 hover:text-blue-900 font-semibold"
          >
            Regístrate aquí
          </a>
          <br />
          <a
            href="/forgot-password"
            className="text-blue-600 hover:text-blue-900 font-semibold"
          >
            ¿Olvidaste tu contraseña?
          </a>
        </>
      }
    />
  );
};

export default LoginPage;