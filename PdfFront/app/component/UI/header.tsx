"use client";

import useAuth from "@/hooks/useAuth";
import Link from "next/dist/client/link";
import { usePathname } from "next/navigation";
import { useState, useEffect, useRef, useMemo } from "react";
import { MdLogout } from "react-icons/md";
import MENU_CONFIG from "./routes";

interface HeaderProps {
    sidebarCollapsed?: boolean;
}

const Header: React.FC<HeaderProps> = ({ sidebarCollapsed = false }) => {
    const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
    const { user, logout, hasRole } = useAuth();
    const pathname = usePathname();
    const menuRef = useRef<HTMLDivElement>(null);

    const menuItems = useMemo(() => {
        return MENU_CONFIG.filter((item) =>
            item.allowedRoles.some((role) => hasRole(role))
        );
    }, [hasRole]);

    const handleLogout = async () => {
        await logout();
    };

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
        <header className="fixed top-0 right-0 left-0 h-14 sm:h-16 bg-white border-b border-gray-200 shadow-sm z-30 flex items-center px-4 sm:px-6 transition-all duration-300 justify-between">
                        {/* Logo Section */}
            <div className="p-4 flex">
                <img
                    src="/img/logo-zona-medica-header.png"
                    alt="Logo Zona Medica"
                    className="object-contain h-12 w-auto"
                />
            </div>
            {/* Nav links */}
            <nav className="flex items-center gap-1">
                {menuItems.map((item) => {
                    const isActive = pathname === item.href;
                    return (
                        <Link
                            key={item.href}
                            href={item.href}
                            className={`
                                flex items-center gap-2 px-2 py-2 rounded-lg text-sm font-medium transition-all
                                ${isActive
                                    ? "bg-blue-100 text-blue-600 border border-blue-300"
                                    : "text-gray-600 hover:bg-gray-100 active:bg-gray-200 border border-transparent"
                                }
                            `}
                        >
                            <span className="shrink-0">{item.icon}</span>
                            <span className="hidden md:block">{item.label}</span>
                        </Link>
                    );
                })}
            </nav>

            {/* Usuario + logout */}
            <div className="relative border border-gray-300 rounded-lg" ref={menuRef}>
                <button
                    onClick={() => setIsProfileMenuOpen(!isProfileMenuOpen)}
                    className="flex items-center gap-2 sm:gap-3 py-2 px-4 rounded-lg text-gray-800 hover:bg-gray-50 transition-colors"
                >
                    <span className="text-sm font-medium max-w-[150px] truncate">
                        {user?.name || "User"}
                    </span>
                </button>

                {isProfileMenuOpen && (
                    <div className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-xl border border-gray-200 py-2 z-50">
                        <div className="px-4 py-3 border-b text-gray-700">
                            <p className="text-sm font-semibold truncate">{user?.name}</p>
                            <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                        </div>
                        <button
                            onClick={handleLogout}
                            className="w-full text-left px-4 py-3 text-sm text-red-600 hover:bg-red-50 transition-colors flex items-center gap-2"
                        >
                            <MdLogout size={16} />
                            <span>Cerrar sesión</span>
                        </button>
                    </div>
                )}
            </div>
        </header>
    );
};

export default Header;