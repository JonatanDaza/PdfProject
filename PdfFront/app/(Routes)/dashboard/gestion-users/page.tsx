"use client";

import React, { useState, useEffect, useCallback, useMemo } from "react";
import { MdPersonAdd, MdRemoveRedEye, MdModeEdit } from "react-icons/md";
import { authService } from "@/services/auth/authService";
import type { UsersDto } from "@/types/api";
import Table, { FilterField } from "@/component/UI/Table/index";
import UserCreateModal from "@/component/modals/UserCreateModal";
import UserViewEditModal from "@/component/modals/UserViewEditModal";

const UsersPage: React.FC = () => {
  const [users,   setUsers]   = useState<UsersDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);

  // Modales
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [modalMode,      setModalMode]      = useState<"view" | "edit">("view");
  const [isViewEditOpen, setIsViewEditOpen] = useState(false);
  const [isCreateOpen,   setIsCreateOpen]   = useState(false);

  const openView = (user: UsersDto) => {
    setSelectedUserId(user.id);
    setModalMode("view");
    setIsViewEditOpen(true);
  };

  const openEdit = (user: UsersDto) => {
    setSelectedUserId(user.id);
    setModalMode("edit");
    setIsViewEditOpen(true);
  };

  // Filtros
  const [filterValues, setFilterValues] = useState<Record<string, string | Date | null>>({
    nombre: "", correo: "", rol: "", estado: "",
  });

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await authService.getAllUsers();
      const raw: any[] = res?.data ?? res ?? [];
      const normalized: UsersDto[] = raw.map((u) => ({
        ...u,
        roles: u.roleDtoList ?? u.roles ?? [],
      }));
      setUsers(normalized);
    } catch (err: any) {
      setError(err?.message ?? "Error cargando usuarios");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const filterFields: FilterField[] = useMemo(() => [
    { id: "nombre",  label: "Nombre",  type: "text",   placeholder: "Buscar...", colSpan: 3 },
    { id: "correo",  label: "Correo",  type: "text",   placeholder: "Buscar...", colSpan: 4 },
    { id: "rol",     label: "Rol",     type: "text",   placeholder: "Buscar...", colSpan: 3 },
    {
      id: "estado", label: "Estado", type: "select", colSpan: 2,
      options: [
        { value: "",        label: "Todos"    },
        { value: "activo",  label: "Activo"   },
        { value: "inactivo",label: "Inactivo" },
      ],
    },
  ], []);

  const handleFilterChange = (id: string, value: string | Date | null) =>
    setFilterValues((prev) => ({ ...prev, [id]: value }));

  const filteredUsers = useMemo(() => users.filter((u) => {
    const name   = u.name?.toLowerCase()  ?? "";
    const email  = u.email?.toLowerCase() ?? "";
    const roles  = u.roles?.map((r) => r.name.toLowerCase()).join(" ") ?? "";

    if (filterValues.nombre && !name.includes((filterValues.nombre   as string).toLowerCase())) return false;
    if (filterValues.correo && !email.includes((filterValues.correo  as string).toLowerCase())) return false;
    if (filterValues.rol    && !roles.includes((filterValues.rol     as string).toLowerCase())) return false;
    if (filterValues.estado) {
      const activo = (filterValues.estado as string) === "activo";
      if (u.active !== activo) return false;
    }
    return true;
  }), [users, filterValues]);

  const columns = useMemo(() => [
    { key: "name"  as keyof UsersDto, header: "Nombre"  },
    { key: "email" as keyof UsersDto, header: "Correo"  },
    {
      key: "roles" as keyof UsersDto,
      header: "Rol",
      render: (row: UsersDto) => (
        <div className="flex flex-wrap gap-1">
          {row.roles?.length ? row.roles.map((r) => (
            <span key={r.id} className="px-2 py-0.5 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-full text-xs font-semibold">
              {r.name}
            </span>
          )) : <span className="text-gray-400 text-xs">Sin rol</span>}
        </div>
      ),
    },
    {
      key: "active" as keyof UsersDto,
      header: "Estado",
      render: (row: UsersDto) => (
        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold
          ${row.active
            ? "bg-emerald-50 text-emerald-700 border border-emerald-200"
            : "bg-gray-100 text-gray-500 border border-gray-200"
          }`}>
          <span className={`w-1.5 h-1.5 rounded-full ${row.active ? "bg-emerald-500" : "bg-gray-400"}`} />
          {row.active ? "Activo" : "Inactivo"}
        </span>
      ),
    },
    {
      key: "id" as keyof UsersDto,
      header: "Acciones",
      render: (row: UsersDto) => (
        <div className="flex justify-center gap-1.5">
          <button
            onClick={() => openView(row)}
            className="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded transition-all"
            title="Ver detalle"
          >
            <MdRemoveRedEye size={16} />
          </button>
          <button
            onClick={() => openEdit(row)}
            className="p-1.5 text-slate-400 hover:text-amber-600 hover:bg-amber-50 rounded transition-all"
            title="Editar"
          >
            <MdModeEdit size={16} />
          </button>
        </div>
      ),
    },
  ], []);

  return (
    <div className="min-h-screen bg-transparent space-y-6 pb-12 animate-in fade-in duration-500 font-sans">

      {/* Header */}
      <div className="bg-white rounded-2xl shadow-sm p-6 border border-gray-100 flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-800">Gestión de Usuarios</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {filteredUsers.length} usuario{filteredUsers.length !== 1 ? "s" : ""} encontrado{filteredUsers.length !== 1 ? "s" : ""}
          </p>
        </div>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="flex items-center gap-2 bg-indigo-600 text-white px-5 py-2 rounded-xl font-bold text-xs shadow-lg shadow-indigo-100 hover:bg-indigo-700 transition-all active:scale-95"
        >
          <MdPersonAdd size={16} /> Nuevo Usuario
        </button>
      </div>

      <Table<UsersDto>
        columns={columns}
        data={filteredUsers}
        pageSize={10}
        filters={filterFields}
        filterValues={filterValues}
        onFilterChange={handleFilterChange}
        onFilterSearch={fetchUsers}
        error={error}
      />

      <UserCreateModal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        onSaved={fetchUsers}
      />
      <UserViewEditModal
        isOpen={isViewEditOpen}
        onClose={() => setIsViewEditOpen(false)}
        userId={selectedUserId}
        mode={modalMode}
        onSaved={fetchUsers}
      />
    </div>
  );
};

export default UsersPage;