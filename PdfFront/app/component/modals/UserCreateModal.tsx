"use client";

import React, { useState, useEffect } from "react";
import { authService } from "@/services/auth/authService";
import type { RoleDto } from "@/types";
import { toast } from "sonner";
import Modal from "../UI/Modal";

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
}

interface UserForm {
  name: string;
  email: string;
  password: string;
  roleId: number | null;
}

const EMPTY: UserForm = { name: "", email: "", password: "", roleId: null };

const UserCreateModal: React.FC<Props> = ({ isOpen, onClose, onSaved }) => {
  const [form,    setForm]    = useState<UserForm>(EMPTY);
  const [roles,   setRoles]   = useState<RoleDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [errors,  setErrors]  = useState<Partial<Record<keyof UserForm, string>>>({});

  useEffect(() => {
    if (!isOpen) return;
    authService.getRoles().then(setRoles).catch(() => toast.error("Error cargando roles"));
  }, [isOpen]);

  const setField = (field: keyof UserForm, value: string | number | null) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const validate = (): boolean => {
    const e: Partial<Record<keyof UserForm, string>> = {};
    if (!form.name.trim())     e.name     = "El nombre es obligatorio";
    if (!form.email.trim())    e.email    = "El correo es obligatorio";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = "Correo inválido";
    if (!form.password.trim()) e.password = "La contraseña es obligatoria";
    else if (form.password.length < 6) e.password = "Mínimo 6 caracteres";
    if (!form.roleId)          e.roleId   = "Selecciona un rol";
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setLoading(true);
    try {
      const selectedRole = roles.find((r) => r.id === form.roleId);
      await authService.adminCreateUser(form.name, form.email, form.password,
        selectedRole ? [selectedRole.name] : undefined);
      toast.success("Usuario creado exitosamente");
      setForm(EMPTY);
      setErrors({});
      onSaved();
      onClose();
    } catch (err: any) {
      toast.error(err?.message ?? "Error al crear el usuario");
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setForm(EMPTY);
    setErrors({});
    onClose();
  };

  const inputClass = (field: keyof UserForm) =>
    `block w-full px-4 py-2.5 bg-gray-50 border text-gray-700 text-sm rounded-xl outline-none transition-all
    ${errors[field]
      ? "border-red-400 focus:ring-2 focus:ring-red-200"
      : "border-gray-200 focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"}`;

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title="Nuevo usuario" size="md">
      <div className="space-y-4">

        {/* Nombre */}
        <div>
          <label className="block mb-1.5 text-xs font-bold text-gray-600 uppercase tracking-wider">
            Nombre completo <span className="text-red-500">*</span>
          </label>
          <input
            type="text"
            value={form.name}
            onChange={(e) => setField("name", e.target.value)}
            placeholder="Ej: Juan Pérez"
            className={inputClass("name")}
          />
          {errors.name && <p className="mt-1 text-xs text-red-500">{errors.name}</p>}
        </div>

        {/* Correo */}
        <div>
          <label className="block mb-1.5 text-xs font-bold text-gray-600 uppercase tracking-wider">
            Correo electrónico <span className="text-red-500">*</span>
          </label>
          <input
            type="email"
            value={form.email}
            onChange={(e) => setField("email", e.target.value)}
            placeholder="correo@zonamedicaips.com"
            className={inputClass("email")}
          />
          {errors.email && <p className="mt-1 text-xs text-red-500">{errors.email}</p>}
        </div>

        {/* Contraseña */}
        <div>
          <label className="block mb-1.5 text-xs font-bold text-gray-600 uppercase tracking-wider">
            Contraseña <span className="text-red-500">*</span>
          </label>
          <input
            type="password"
            value={form.password}
            onChange={(e) => setField("password", e.target.value)}
            placeholder="Mínimo 6 caracteres"
            className={inputClass("password")}
          />
          {errors.password && <p className="mt-1 text-xs text-red-500">{errors.password}</p>}
        </div>

        {/* Rol */}
        <div>
          <label className="block mb-1.5 text-xs font-bold text-gray-600 uppercase tracking-wider">
            Rol <span className="text-red-500">*</span>
          </label>
          <select
            value={form.roleId ?? ""}
            onChange={(e) => setField("roleId", e.target.value ? Number(e.target.value) : null)}
            className={inputClass("roleId")}
          >
            <option value="">Seleccionar rol...</option>
            {roles.map((r) => (
              <option key={r.id} value={r.id}>{r.name}</option>
            ))}
          </select>
          {errors.roleId && <p className="mt-1 text-xs text-red-500">{errors.roleId}</p>}
        </div>

        {/* Acciones */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            onClick={handleClose}
            className="px-4 py-2 text-sm font-semibold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-100 transition"
          >
            Cancelar
          </button>
          <button
            onClick={handleSubmit}
            disabled={loading}
            className="px-5 py-2 text-sm font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 transition disabled:opacity-50 flex items-center gap-2"
          >
            {loading && (
              <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
              </svg>
            )}
            {loading ? "Guardando..." : "Crear usuario"}
          </button>
        </div>
      </div>
    </Modal>
  );
};

export default UserCreateModal;