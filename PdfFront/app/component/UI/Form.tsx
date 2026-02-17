"use client";

import { ReactNode, FormEvent } from "react";
import Image from "next/image";

interface FormField {
  id: string;
  label: string;
  type: "email" | "text" | "password" | "number" | "tel" | "textarea" | "select";
  placeholder?: string;
  required?: boolean;
  disabled?: boolean;
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;           // ← nuevo
  errorMessage?: string | null;  // ← nuevo
  hint?: ReactNode;              // ← nuevo: contenido libre debajo del input
  options?: { label: string; value: string }[]; // solo para type="select"
  suffix?: ReactNode; // para botones dentro del input, como el ojo de password
}

interface FormProps {
  img?: string | null;
  title: string | null;
  subtitle?: string;
  fields: FormField[];
  onSubmit: (e: FormEvent) => void;
  submitLabel?: string;
  isLoading?: boolean;
  loadingLabel?: string;
  error?: string;
  canSubmit?: boolean;           // ← nuevo
  footer?: ReactNode;
  children?: ReactNode; // para contenido extra entre campos y botón
}

const Form = ({
  img,
  title,
  subtitle,
  fields,
  onSubmit,
  submitLabel = "Enviar",
  isLoading = false,
  loadingLabel = "Cargando...",
  error,
  canSubmit = true,
  footer,
  children,
}: FormProps) => {
  const inputClass =
    "w-full px-4 py-3 rounded-lg bg-[#444] text-white border border-[#555] focus:outline-none focus:ring-2 focus:ring-[#61dafb]";

  return (
    <div className="flex flex-col min-h-screen">
      <main className="flex-1 font-poppins text-white flex flex-col items-center justify-center p-4">
        <div className="border rounded-xl shadow-2xl p-10 w-full max-w-lg">

          {/* Header */}
          <div className="flex flex-col items-center mb-8">
            {img && (
              <div className="mb-4">
                <Image
                  src={img}
                  alt="Logo"
                  width={100}
                  height={100}
                  priority
                  className="object-contain"
                />
              </div>
            )}
            {title && (
              <h1 className="text-4xl font-bold mb-2 text-black">{title}</h1>
            )}
            {subtitle && (
              <p className="text-gray-400 text-sm">{subtitle}</p>
            )}
          </div>

          <form className="space-y-5" onSubmit={onSubmit}>

            {/* Campos dinámicos */}
            {fields.map((field) => (
              <div key={field.id}>
                <label
                  htmlFor={field.id}
                  className="block mb-2 font-semibold text-gray-500"
                >
                  {field.label}
                </label>

                {field.type === "textarea" ? (
                  <textarea
                    id={field.id}
                    required={field.required}
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value)}
                    onBlur={field.onBlur}
                    className={`${inputClass} resize-none min-h-[100px]`}
                    placeholder={field.placeholder}
                    disabled={field.disabled || isLoading}
                  />
                ) : field.type === "select" ? (
                  <select
                    id={field.id}
                    required={field.required}
                    value={field.value}
                    onChange={(e) => field.onChange(e.target.value)}
                    onBlur={field.onBlur}
                    className={inputClass}
                    disabled={field.disabled || isLoading}
                  >
                    <option value="" disabled>
                      {field.placeholder || "Selecciona una opción"}
                    </option>
                    {field.options?.map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
                  </select>
                ) : (
                  <div className="relative">
                    <input
                      type={field.type}
                      id={field.id}
                      required={field.required}
                      value={field.value}
                      onChange={(e) => field.onChange(e.target.value)}
                      onBlur={field.onBlur}
                      className={`${inputClass} ${field.suffix ? "pr-12" : ""}`}
                      placeholder={field.placeholder}
                      disabled={field.disabled || isLoading}
                    />
                    {field.suffix && (
                      <div className="absolute right-3 top-1/2 -translate-y-1/2">
                        {field.suffix}
                      </div>
                    )}
                  </div>
                )}

                {/* Error inline por campo */}
                {field.errorMessage && (
                  <p className="mt-1 text-xs text-red-400">{field.errorMessage}</p>
                )}

                {/* Hint libre (ej: indicador de fortaleza de contraseña) */}
                {field.hint}
              </div>
            ))}

            {/* Slot para contenido extra */}
            {children}

            {/* Error */}
            {error && (
              <div className="text-red-400 text-sm text-center bg-red-900/20 py-2 px-3 rounded-lg">
                {error}
              </div>
            )}

            {/* Botón submit */}
            <button
              type="submit"
              disabled={isLoading || !canSubmit}
              className="w-full text-black hover:text-blue-800 py-3 rounded-lg transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? loadingLabel : submitLabel}
            </button>

            {/* Footer (links, textos extra) */}
            {footer && (
              <div className="text-center text-sm mt-4">
                {footer}
              </div>
            )}
          </form>
        </div>
      </main>
    </div>
  );
};

export default Form;