"use client";

import useAuth from "@/hooks/useAuth";
import { useState, useEffect, useRef } from "react";

interface HeaderProps {
    sidebarCollapsed?: boolean;
}

const Header: React.FC<HeaderProps> = ({ sidebarCollapsed = false }) => {
    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
    const { user, logout } = useAuth();
    const menuRef = useRef<HTMLDivElement>(null);

    const getInitials = (name?: string): string => {
        if (!name) return "U";
        const parts = name.split(" ");
        if (parts.length >= 2) {
            return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
        }
        return parts[0][0].toUpperCase();
    };

    const userInitials = getInitials(user?.name);

    const handleLogout = async () => {
        await logout();
    };

    // Cerrar menú al hacer click fuera
    useEffect(() => {
        const handleClickOutside = (event: MouseEvent) => {
            if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
                setIsProfileMenuOpen(false);
            }
        };

        if (isProfileMenuOpen) {
            document.addEventListener("mousedown", handleClickOutside);
        }

        return () => {
            document.removeEventListener("mousedown", handleClickOutside);
        };
    }, [isProfileMenuOpen]);

    return (
        <header 
            className={`
                fixed top-0 h-14 sm:h-16 bg-white border-b border-l border-gray-200 shadow-sm
                z-30 flex items-center px-4 sm:px-6 transition-all duration-300 justify-between
                ${sidebarCollapsed ? 'left-20' : 'left-64'}
                right-0
            `}
        >
            <div className="flex items-center gap-2 sm:gap-4" />

            {/* Menú de usuario */}
            <div className="relative  border border-gray-300 rounded-lg" ref={menuRef}>
                <button
                    onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
                    className="flex items-center gap-2 sm:gap-3 bg-white/10 min-h-auto py-2 px-4 sm:px-8 rounded-lg text-white transition-colors active:bg-white/30"
                >
                    <span className="hidden sm:block text-gray-800 text-sm font-medium max-w-37.5 truncate">
                        {user?.name || "User"}
                    </span>
                    {/* <div className="w-8 h-8 sm:w-9 sm:h-9 bg-blue-200 rounded-full flex items-center justify-center font-bold text-sm">
                        {userInitials}
                    </div> */}
                </button>

                {/* Dropdown Menu */}
                {/* {isProfileMenuOpen && (
                    <div className="absolute right-0 mt-2 w-56 sm:w-64 bg-white rounded-lg shadow-xl border border-gray-200 py-2 z-50">
                        <div className="px-4 py-3 border-b text-gray-700">
                            <p className="text-sm font-semibold truncate">{user?.name}</p>
                            <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="w-full text-left px-4 py-3 text-sm text-red-600 hover:bg-red-50 transition-colors active:bg-red-100 flex items-center gap-2"
                        >
                            <span>🚪</span>
                            <span>Cerrar sesión</span>
                        </button>
                    </div>
                )} */}
            </div>
        </header>
    );
}

export default Header;