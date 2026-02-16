import React from "react";
import type { DataHC, HCProps } from "./types";

function HC<T extends object>({
    columns,
    row,
    isDarkMode = false,
    className = "",
}: HCProps<T>) {
    // Clases para el contenedor principal
    const containerClasses = isDarkMode
        ? "bg-shadowBlue border-grayText"
        : "bg-gradient-to-br from-white via-gray-50 to-white border-gray-200";

    // Clases para el contenido de los campos
    const contentClasses = isDarkMode
        ? "bg-darkBlue/30 border-grayText/30 text-white"
        : "bg-gray-50/50 border-gray-200 text-gray-900";

    // Clases para las etiquetas
    const labelClasses = isDarkMode
        ? "text-gray-300"
        : "text-gray-600";

    return (
        <div className="max-w-6xl mx-auto p-3 sm:p-6">
            <div
                className={`w-full rounded-2xl sm:rounded-3xl border shadow-lg sm:shadow-xl backdrop-blur-sm transition-all duration-500 hover:shadow-2xl ${containerClasses} ${className}`}
            >
                <div className="p-3 sm:p-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
                        {columns.map((col, index) => {
                            const value = col.render 
                                ? col.render(row)
                                : row[col.key as keyof T];
                            
                            return (
                                <div 
                                    key={index} 
                                    className={`flex flex-col gap-2 ${col.className || ""}`}
                                >
                                    {/* Label del campo */}
                                    <label
                                        className={`text-xs sm:text-sm font-semibold uppercase tracking-wider transition-colors duration-200 ${labelClasses}`}
                                    >
                                        {col.field}
                                    </label>
                                    {/* Valor del campo */}
                                    <div
                                        className={`w-full rounded-lg sm:rounded-xl border py-2 sm:py-3 px-3 sm:px-4 text-sm sm:text-base backdrop-blur-sm transition-all duration-300 flex items-center whitespace-pre-wrap ${contentClasses}`}
                                    >
                                        {value ? (
                                            String(value)
                                        ) : (
                                            <span className="opacity-50">Sin información</span>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default HC;