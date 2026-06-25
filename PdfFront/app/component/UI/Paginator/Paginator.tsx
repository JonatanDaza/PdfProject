import React from "react";
import { PaginationProps } from "./types";

const Paginator: React.FC<PaginationProps> = ({
    page,
    totalPages,
    onPageChange,
    isDarkMode,
}) => {
    return (
        <div
            className={`flex flex-col sm:flex-row items-center justify-between gap-3 p-4 sm:p-6 ${isDarkMode ? "border-slate-600" : "border-gray-200"} border-opacity-20 border-t`}
        >
            <div
                className={`text-xs sm:text-sm font-medium ${isDarkMode ? "text-slate-300" : "text-gray-600"}`}
            >
                Página <span className="font-bold text-blue-500">{page}</span> de{" "}
                <span className="font-bold text-blue-500">{totalPages}</span>
            </div>
            <div className="flex items-center gap-2">
                <button
                    onClick={() => onPageChange(page - 1)}
                    disabled={page === 1}
                    className={`flex items-center justify-center rounded-lg px-3 sm:px-4 py-2 text-xs sm:text-sm font-medium transition-all duration-200 hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed ${isDarkMode
                        ? "border border-slate-600 bg-slate-700 text-slate-200 hover:bg-slate-600"
                        : "border border-gray-300 bg-white text-gray-700 shadow-sm hover:bg-gray-50"
                        }`}
                    aria-label="Página anterior"
                >
                    <svg className="w-4 h-4 sm:hidden" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                    </svg>
                    <span className="hidden sm:inline">Anterior</span>
                </button>
                <div
                    className={`rounded-lg px-3 sm:px-4 py-2 text-xs sm:text-sm font-bold shadow-sm min-w-[60px] sm:min-w-[80px] text-center ${isDarkMode
                        ? "border border-blue-500 bg-blue-600 text-white"
                        : "border border-blue-400 bg-blue-500 text-white"
                        }`}
                >
                    {page} / {totalPages}
                </div>
                <button
                    onClick={() => onPageChange(page + 1)}
                    disabled={page === totalPages}
                    className={`flex items-center justify-center rounded-lg px-3 sm:px-4 py-2 text-xs sm:text-sm font-medium transition-all duration-200 hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed ${isDarkMode
                        ? "border border-slate-600 bg-slate-700 text-slate-200 hover:bg-slate-600"
                        : "border border-gray-300 bg-white text-gray-700 shadow-sm hover:bg-gray-50"
                        }`}
                    aria-label="Página siguiente"
                >
                    <span className="hidden sm:inline">Siguiente</span>
                    <svg className="w-4 h-4 sm:hidden" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                </button>
            </div>
        </div>
    );
};

export default Paginator;