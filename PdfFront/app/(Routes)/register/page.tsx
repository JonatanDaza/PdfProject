"use client";

import { authService } from "@/services/auth/authService";
import { useRouter } from "next/navigation";
import { useState } from "react";
import {
  isFormValid,
  registerValidations,
  validateField,
  passwordHints,
  getPasswordStrength,
  strengthConfig
} from "@/component/auth/Formvalidation";
import Form from "@/component/UI/Form";

const RegisterPage = () => {
  const [name, setName] = useState("");
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
    touched[id] ? validateField(id, value, registerValidations) : null;

  const canSubmit = isFormValid({ name, email, password }, registerValidations);

  const strength = getPasswordStrength(password);
  const { label: strengthLabel, color: strengthColor, bars } = strengthConfig[strength];
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isLoading) return;
    
    setError("");
    setIsLoading(true);

    try {
      const response = await authService.register(name, email, password);
      
      if (response?.data) {
        
        // Clear any existing error messages
        setError("");
        router.push("/login");
      }
    } catch (err: any) {
      setError(err.message || "Error al registrarse");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Form
      title={null}
      img="/img/logo-zona-medica-header.png"
      subtitle={"Crea tu cuenta"}
      onSubmit={handleSubmit}
      submitLabel="Registrarse"
      isLoading={isLoading}
      loadingLabel="Creando cuenta..."
      error={error}
      canSubmit={canSubmit}
      fields={[
        {
          id: "name",
          label: "Nombre completo",
          type: "text",
          placeholder: "Tu nombre",
          required: true,
          value: name,
          onChange: setName,
          onBlur: () => handleBlur("name"),
          errorMessage: fieldError("name", name),
        },
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
          placeholder: "Crea una contraseña",
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
          // Indicador de fortaleza + sugerencias debajo del input
          hint: password.length > 0 ? (
            <div className="mt-2 space-y-2">

              {/* Barras de fortaleza */}
              <div className="flex items-center gap-2">
                <div className="flex gap-1 flex-1">
                  {[1, 2, 3, 4].map((bar) => (
                    <div
                      key={bar}
                      className={`h-1 flex-1 rounded-full transition-all duration-300 ${
                        bar <= bars ? strengthColor : "bg-gray-600"
                      }`}
                    />
                  ))}
                </div>
                {strengthLabel && (
                  <span className="text-xs text-gray-400 w-16 text-right">
                    {strengthLabel}
                  </span>
                )}
              </div>

              {/* Checklist de sugerencias */}
              <ul className="space-y-1">
                {passwordHints.map((hint) => {
                  const met = hint.met(password);
                  return (
                    <li
                      key={hint.label}
                      className={`flex items-center gap-1.5 text-xs transition-colors duration-200 ${
                        met ? "text-green-400" : "text-gray-500"
                      }`}
                    >
                      <span>{met ? "✓" : "·"}</span>
                      {hint.label}
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : null,
        },
      ]}
      footer={
        <>
          <span className="text-gray-400">¿Ya tienes una cuenta?</span>{" "}
          <a
            href="/login"
            className="text-blue-600 hover:text-blue-900 font-semibold"
          >
            Inicia sesión aquí
          </a>
        </>
      }
    />
  );
};

export default RegisterPage;