"use client";

import React from "react";

interface HeaderProps {
    toggleSidebar: () => void;
}

const Header: React.FC<HeaderProps> = ({ toggleSidebar }) => {
    return (
        <header className="fixed top-0 left-0 lg:left-64 right-0 h-16 bg-white shadow-md z-50 flex items-center px-6 transition-all duration-300">
            {/* Botón hamburguesa - solo visible en móvil */}
            <button 
                onClick={toggleSidebar}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors lg:hidden"
                aria-label="Toggle menu"
            >
                <div className="w-6 h-5 flex flex-col justify-between">
                    <span className="w-full h-0.5 bg-gray-700 rounded"></span>
                    <span className="w-full h-0.5 bg-gray-700 rounded"></span>
                    <span className="w-full h-0.5 bg-gray-700 rounded"></span>
                </div>
            </button>

            {/* Título */}
            <h1 className="ml-4 text-xl font-semibold text-gray-800">
                Sistema de Historias Clínicas
            </h1>
        </header>
    );
}

export default Header;