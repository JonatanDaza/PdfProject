import React, { useState, useMemo, useEffect } from "react";
import type { TableProps } from "./types";
import { Frown } from "lucide-react";
import Paginator from "../Paginator/Paginator";
import type { PaginationProps } from "../Paginator/types";

// Tipos para el sistema de filtros
export interface FilterField {
  id: string;
  label: string;
  type: "text" | "select" | "date";
  placeholder?: string;
  options?: { label: string; value: string }[];
  colSpan?: number;
}

export interface TableFiltersProps {
  fields: FilterField[];
  values: Record<string, string | Date | null>;
  onChange: (id: string, value: string | Date | null) => void;
  onSearch?: () => void;
}

function Table<T extends object>({
  columns,
  data,
  isDarkMode = false,
  pageSize = 5,
  filterPlaceholder = "Buscar...",
  filterFunction,
  className = "",
  onAddClick,
  addButtonText = "Agregar",
  paginator = Paginator,
  externalPage,
  onExternalPageChange,
  externalTotalPages,
  onSearchChange,
  onRowClick,
  // ← nuevas props de filtros
  filters,
  filterValues,
  onFilterChange,
  onFilterSearch,
  error,
}: TableProps<T> & {
  onAddClick?: () => void;
  addButtonText?: string;
  paginator?: React.FC<PaginationProps>;
  onSearchChange?: (search: string) => void;
  onRowClick?: (row: T) => void;
  filters?: FilterField[];
  filterValues?: Record<string, string | Date | null>;
  onFilterChange?: (id: string, value: string | Date | null) => void;
  onFilterSearch?: () => void;
  error?: string | null;
}) {
  const [page, setPage] = useState(1);

  const isServerControlled = typeof externalTotalPages === "number";
  const currentPage = externalPage ?? page;

  const totalPages = isServerControlled
    ? Math.max(1, externalTotalPages as number)
    : Math.ceil(data.length / pageSize) || 1;

  const paginatedData = useMemo(() => {
    if (isServerControlled) return data;
    return data.slice((currentPage - 1) * pageSize, currentPage * pageSize);
  }, [currentPage, pageSize, data, isServerControlled]);

  useEffect(() => {
    if (externalPage === undefined) setPage(1);
  }, [data, externalPage]);

  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || newPage > totalPages) return;
    if (typeof onExternalPageChange === "function") {
      onExternalPageChange(newPage);
    } else {
      setPage(newPage);
    }
  };

  const containerClasses = isDarkMode ? "bg-shadowBlue border-grayText" : "bg-white border-gray-200";
  const headerClasses    = isDarkMode ? "bg-shadowBlue text-white border-grayText" : "bg-gray-50 text-gray-600 border-gray-200";
  const tbodyClasses     = isDarkMode ? "divide-grayText/20 bg-shadowBlue" : "divide-gray-100 bg-white";
  const tableBackground  = isDarkMode ? "bg-shadowBlue" : "bg-white";
  const getRowClasses    = (isDark: boolean) => isDark
    ? "border-grayText/30 hover:bg-darkBlue/30 bg-shadowBlue"
    : "border-gray-100 hover:bg-slate-50 bg-white";

  return (
    <div className={`w-full rounded-xl border shadow-sm transition-all duration-300 ${containerClasses} ${className}`}>

      {/* ── Panel de filtros (si se pasan) ── */}
      {filters && filters.length > 0 && filterValues && onFilterChange && (
        <div className="p-4 border-b border-gray-100">
          <div className="grid gap-3 items-end" style={{ gridTemplateColumns: "repeat(12, minmax(0, 1fr))" }}>

            {filters.map((field) => (
              <div
                key={field.id}
                style={{ gridColumn: `span ${field.colSpan ?? 2}` }}
              >
                {field.type === "text" && (
                  <>
                    <label className="block mb-1.5 text-[10px] font-bold text-gray-600 uppercase tracking-wider">
                      {field.label}
                    </label>
                    <input
                      id={field.id}
                      type="text"
                      placeholder={field.placeholder ?? "Buscar..."}
                      value={(filterValues[field.id] as string) ?? ""}
                      onChange={(e) => onFilterChange(field.id, e.target.value)}
                      className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-lg text-gray-700 placeholder-gray-400 focus:outline-none focus:ring-1 focus:ring-indigo-400 focus:border-indigo-400 transition-all"
                    />
                  </>
                )}

                {field.type === "select" && (
                  <>
                    <label className="block mb-1.5 text-[10px] font-bold text-gray-600 uppercase tracking-wider">
                      {field.label}
                    </label>
                    <select
                      id={field.id}
                      value={(filterValues[field.id] as string) ?? ""}
                      onChange={(e) => onFilterChange(field.id, e.target.value)}
                      className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-lg text-gray-700 focus:outline-none focus:ring-1 focus:ring-indigo-400 focus:border-indigo-400 transition-all"
                    >
                      {field.options?.map((opt) => (
                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                      ))}
                    </select>
                  </>
                )}

                {field.type === "date" && (
                  <>
                    <label className="block mb-1.5 text-[10px] font-bold text-gray-600 uppercase tracking-wider">
                      {field.label}
                    </label>
                    {/* DatePicker se importa en la page y se pasa como children o se usa input nativo */}
                    <input
                      id={field.id}
                      type="date"
                      value={(filterValues[field.id] as string) ?? ""}
                      onChange={(e) => onFilterChange(field.id, e.target.value)}
                      className="w-full px-3 py-2 text-xs bg-gray-50 border border-gray-200 rounded-lg text-gray-700 focus:outline-none focus:ring-1 focus:ring-indigo-400 focus:border-indigo-400 transition-all"
                    />
                  </>
                )}

              </div>
            ))}
            {/* Botón buscar */}
            {/* {onFilterSearch && (
              <div className="lg:col-span-1">
                <button
                  onClick={onFilterSearch}
                  className="w-full bg-indigo-600 text-white p-2 rounded-lg flex justify-center items-center hover:bg-indigo-700 transition shadow-sm active:scale-95 "
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z" />
                  </svg>
                </button>
              </div>
            )} */}
          </div>

          {/* Error inline */}
          {error && (
            <div className="mt-3 bg-red-50 border border-red-200 text-red-600 text-xs px-3 py-2 rounded-lg">
              {error}
            </div>
          )}
        </div>
      )}

      {/* ── Tabla ── */}
      <div className="overflow-hidden rounded-b-xl">
        <div className="overflow-x-auto">
          <table className={`min-w-full ${tableBackground}`}>
            <thead>
              <tr className={`border-y ${headerClasses}`}>
                {columns.map((col) => (
                  <th
                    key={String(col.key)}
                    className={`px-3 sm:px-5 py-2.5 text-center text-[10px] font-semibold tracking-wider uppercase ${col.className || ""}`}
                  >
                    {col.header}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody className={`${tbodyClasses} divide-y`}>
              {paginatedData.length === 0 ? (
                <tr className={tableBackground}>
                  <td colSpan={columns.length} className="px-4 py-8 text-center text-gray-400">
                    <div className="flex flex-col items-center gap-2">
                      <Frown className="h-8 w-8 opacity-40" />
                      <span className="text-xs font-medium">No se encontraron resultados</span>
                      <span className="text-[10px] opacity-50">Intenta con otros términos de búsqueda</span>
                    </div>
                  </td>
                </tr>
              ) : (
                paginatedData.map((row, idx) => (
                  <tr
                    key={idx}
                    className={`transition-colors duration-150 ${getRowClasses(isDarkMode)} ${typeof onRowClick === "function" ? "cursor-pointer" : ""}`}
                    onClick={typeof onRowClick === "function" ? () => onRowClick(row) : undefined}
                  >
                    {columns.map((col) => (
                      <td
                        key={String(col.key)}
                        className={`px-3 sm:px-5 py-2.5 text-xs ${isDarkMode ? "text-white" : "text-gray-600"} ${col.className || ""}`}
                      >
                        {col.render ? col.render(row) : String(row[col.key as keyof T])}
                      </td>
                    ))}
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Paginación */}
      {totalPages > 0 && paginator &&
        React.createElement(paginator, {
          page: currentPage,
          totalPages,
          onPageChange: handlePageChange,
          isDarkMode,
        })}
    </div>
  );
}

export default Table;