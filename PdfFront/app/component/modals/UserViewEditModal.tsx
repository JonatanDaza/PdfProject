"use client";

import React, { useState, useEffect } from "react";
import { authService } from "@/services/auth/authService";
import type { UsersDto, RoleDto } from "@/types";
import { toast } from "sonner";
import Modal from "../UI/Modal";

interface Props {
  isOpen: boolean;
  onClose: () => void;
  userId: number | null;
  mode: "view" | "edit";
  onSaved: () => void;
}

const Field: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div>
    <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">{label}</p>
    <p className="text-sm text-gray-800 bg-gray-50 px-4 py-2.5 rounded-xl border border-gray-200">{value || "—"}</p>
  </div>
);

const UserViewEditModal: React.FC<Props> = ({ isOpen, onClose, userId, mode, onSaved }) => {
  const [user,             setUser]             = useState<UsersDto | null>(null);
  const [roles,            setRoles]            = useState<RoleDto[]>([]);
  const [selectedRoleId,   setSelectedRoleId]   = useState<number | null>(null);
  const [loading,          setLoading]          = useState(false);
  const [fetching,         setFetching]         = useState(false);

  // Cambio de contraseña
  const [newPassword,      setNewPassword]      = useState("");
  const [showPassword,     setShowPassword]     = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);

useEffect(() => {
    if (!isOpen || !userId) return;
    setFetching(true);
    Promise.all([
      authService.getUserById(userId),
      authService.getRoles(),
    ])
      .then(([userData, rolesData]) => {
        const u = userData?.data ?? userData;
        const normalized: UsersDto = {
          ...u,
          roles: u.roleDtoList ?? u.roles ?? [],
        };
        setUser(normalized);
        setSelectedRoleId(normalized.roles?.[0]?.id ?? null);
        setRoles(rolesData);
      })
      .catch(() => toast.error("Error cargando datos del usuario"))
      .finally(() => setFetching(false));
  }, [isOpen, userId]);

  const handleToggleActive = async () => {
    if (!user) return;
    setLoading(true);
    try {
      if (user.active) {
        await authService.deactivateUser(user.id);
        toast.success("Usuario desactivado");
      } else {
        await authService.activateUser(user.id);
        toast.success("Usuario activado");
      }
      onSaved();
      onClose();
    } catch (err: any) {
      toast.error(err?.message ?? "Error al cambiar estado");
    } finally {
      setLoading(false);
    }
  };

  const handleChangeRole = async () => {
    if (!user || !selectedRoleId) return;
    setLoading(true);
    try {
      await authService.changeRole(user.id, selectedRoleId);
      toast.success("Rol actualizado exitosamente");
      onSaved();
      onClose();
    } catch (err: any) {
      toast.error(err?.message ?? "Error al cambiar rol");
    } finally {
      setLoading(false);
    }
  };

  const handleChangePassword = async () => {
    if (!user || newPassword.length < 8) return;
    setChangingPassword(true);
    try {
      await authService.changePassword(user.id, newPassword);
      toast.success("Contraseña actualizada exitosamente");
      setNewPassword("");
      setShowPassword(false);
    } catch (err: any) {
      toast.error(err?.message ?? "Error al cambiar la contraseña");
    } finally {
      setChangingPassword(false);
    }
  };

  const handleClose = () => {
    setUser(null);
    setSelectedRoleId(null);
    setNewPassword("");
    setShowPassword(false);
    onClose();
  };

  const title = mode === "view" ? "Detalle de usuario" : "Editar usuario";

  return (
    <Modal isOpen={isOpen} onClose={handleClose} title={title} size="md">
      {fetching ? (
        <div className="flex justify-center items-center py-12">
          <svg className="animate-spin h-6 w-6 text-indigo-500" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
          </svg>
        </div>
      ) : !user ? (
        <p className="text-sm text-gray-500 text-center py-8">No se encontró el usuario.</p>
      ) : (
        <div className="space-y-4">

          {/* Info básica */}
          <Field label="Nombre"            value={user.name} />
          <Field label="Correo electrónico" value={user.email} />

          {/* Estado */}
          <div>
            <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">Estado</p>
            <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold
              ${user.active
                ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
                : "bg-gray-100 text-gray-500 border border-gray-200"
              }`}>
              <span className={`w-1.5 h-1.5 rounded-full ${user.active ? "bg-emerald-500" : "bg-gray-400"}`} />
              {user.active ? "Activo" : "Inactivo"}
            </span>
          </div>

          {/* Rol actual */}
          <div>
            <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">Rol actual</p>
            <div className="flex flex-wrap gap-1.5">
              {user.roles?.length ? user.roles.map((r) => (
                <span key={r.id} className="px-2.5 py-1 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-full text-xs font-semibold">
                  {r.name}
                </span>
              )) : <span className="text-sm text-gray-400">Sin rol asignado</span>}
            </div>
          </div>

          {/* Edición */}
          {mode === "edit" && (
            <>
              {/* Cambiar rol */}
              <div>
                <label className="block mb-1.5 text-xs font-bold text-gray-600 uppercase tracking-wider">
                  Cambiar rol
                </label>
                <select
                  value={selectedRoleId ?? ""}
                  onChange={(e) => setSelectedRoleId(e.target.value ? Number(e.target.value) : null)}
                  className="block w-full px-4 py-2.5 bg-gray-50 border border-gray-200 text-gray-700 text-sm rounded-xl outline-none transition-all focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                >
                  <option value="">Seleccionar rol...</option>
                  {roles.map((r) => (
                    <option key={r.id} value={r.id}>{r.name}</option>
                  ))}
                </select>
              </div>

              {/* Cambiar contraseña */}
              <div className="pt-2 border-t border-gray-100">
                <label className="block mb-1.5 text-xs font-bold text-gray-600 uppercase tracking-wider">
                  Cambiar contraseña
                </label>
                <div className="flex items-center gap-2">
                  <div className="relative flex-1">
                    <input
                      type={showPassword ? "text" : "password"}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Mínimo 8 caracteres"
                      className="block w-full px-4 py-2.5 pr-10 bg-gray-50 border border-gray-200 text-gray-700 text-sm rounded-xl outline-none transition-all focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500"
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
                    >
                      {showPassword ? "🙈" : "👁️"}
                    </button>
                  </div>
                  <button
                    onClick={handleChangePassword}
                    disabled={changingPassword || newPassword.length < 8}
                    className="px-4 py-2.5 text-sm font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 transition disabled:opacity-50 whitespace-nowrap flex items-center gap-2"
                  >
                    {changingPassword && (
                      <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                      </svg>
                    )}
                    Cambiar
                  </button>
                </div>
                {newPassword.length > 0 && newPassword.length < 8 && (
                  <p className="mt-1 text-xs text-red-500">Debe tener al menos 8 caracteres</p>
                )}
              </div>

              {/* Acciones edición */}
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 pt-2 border-t border-gray-100">
                <button
                  onClick={handleToggleActive}
                  disabled={loading}
                  className={`px-4 py-2 text-sm font-semibold rounded-xl transition disabled:opacity-50
                    ${user.active
                      ? "text-red-600 bg-red-50 border border-red-200 hover:bg-red-100"
                      : "text-emerald-600 bg-emerald-50 border border-emerald-200 hover:bg-emerald-100"
                    }`}
                >
                  {user.active ? "Desactivar usuario" : "Activar usuario"}
                </button>

                <div className="flex items-center gap-3">
                  <button
                    onClick={handleClose}
                    className="px-4 py-2 text-sm font-semibold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-100 transition"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={handleChangeRole}
                    disabled={loading || !selectedRoleId}
                    className="px-5 py-2 text-sm font-semibold text-white bg-indigo-600 rounded-xl hover:bg-indigo-700 transition disabled:opacity-50 flex items-center gap-2"
                  >
                    {loading && (
                      <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z" />
                      </svg>
                    )}
                    {loading ? "Guardando..." : "Guardar cambios"}
                  </button>
                </div>
              </div>
            </>
          )}

          {/* Solo vista */}
          {mode === "view" && (
            <div className="flex justify-end pt-2 border-t border-gray-100">
              <button
                onClick={handleClose}
                className="px-4 py-2 text-sm font-semibold text-gray-600 bg-white border border-gray-200 rounded-xl hover:bg-gray-100 transition"
              >
                Cerrar
              </button>
            </div>
          )}
        </div>
      )}
    </Modal>
  );
};

export default UserViewEditModal;