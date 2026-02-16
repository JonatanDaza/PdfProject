"use client";

import React, { useState, useMemo, useEffect } from "react";
import { usePathname } from "next/navigation";
import useAuth from "@/hooks/useAuth";
import Link from "next/link";
import { 
    MdDashboard, 
    MdCloudUpload, 
    MdHistory, 
    MdLogout
} from "react-icons/md";

interface MenuItem {
    href: string;
    label: string;
    icon: React.ReactNode;
}

interface SidebarProps {
    onCollapsedChange?: (collapsed: boolean) => void;
}

const MENU_CONFIG: Record<string, MenuItem[]> = {
    admin: [
        { href: "/dashboard", label: "Dashboard", icon: <MdDashboard size={24} /> },
        { href: "/dashboard/uploadHC", label: "Subir HC", icon: <MdCloudUpload size={24} /> },
        { href: "/dashboard/historias", label: "Historias", icon: <MdHistory size={24} /> },
    ],
    user: [
        { href: "/dashboard", label: "Dashboard", icon: <MdDashboard size={24} /> },
        { href: "/dashboard/uploadHC", label: "Subir HC", icon: <MdCloudUpload size={24} /> },
        { href: "/dashboard/historias", label: "Historias", icon: <MdHistory size={24} /> },
    ],
};

const Sidebar: React.FC<SidebarProps> = ({ onCollapsedChange }) => {
    const [isCollapsed, setIsCollapsed] = useState(false);
    const pathname = usePathname();
    const { logout, hasRole } = useAuth();

    const role = hasRole("admin") ? "admin" : "user";
    const menuItems = useMemo(() => MENU_CONFIG[role], [role]);

    const handleLogout = async () => {
        await logout();
    };

    // Detectar cambios de tamaño de pantalla
    useEffect(() => {
        const handleResize = () => {
            const width = window.innerWidth;
            const shouldCollapse = width < 1024; // lg breakpoint
            setIsCollapsed(shouldCollapse);
            onCollapsedChange?.(shouldCollapse);
        };

        // Ejecutar al montar
        handleResize();

        // Escuchar cambios de tamaño
        window.addEventListener('resize', handleResize);

        return () => window.removeEventListener('resize', handleResize);
    }, [onCollapsedChange]);

    return (
        <aside 
            className={`
                fixed top-0 left-0 bottom-0 bg-white shadow-lg z-40
                transition-all duration-300 ease-in-out
                flex flex-col
                ${isCollapsed ? 'w-20' : 'w-64'}
            `}
        >
            {/* Logo Section */}
            <div className="p-4 flex items-center justify-center">
                {!isCollapsed ? (
                    <img
                        src="/img/logo-zona-medica-header.png"
                        alt="Logo Zona Medica"
                        className="object-contain h-12 w-auto"
                    />
                ) : (
                    <img
                        src="/img/logo.png"
                        alt="Logo"
                        className="object-contain h-10 w-10"
                    />
                )}
            </div>

            {/* Menu Items */}
            <nav className="flex-1 p-4 overflow-y-auto">
                <ul className="space-y-2">
                    {menuItems.map((item) => {
                        const isActive = pathname === item.href;
                        return (
                            <li key={item.href}>
                                <Link 
                                    href={item.href}
                                    className={`
                                        flex items-center gap-3 px-4 py-3 rounded-lg transition-all
                                        ${isActive 
                                            ? 'bg-blue-100 text-blue-600 font-semibold' 
                                            : 'text-gray-700 hover:bg-gray-100 active:bg-gray-200'
                                        }
                                        ${isCollapsed ? 'justify-center' : ''}
                                    `}
                                    title={isCollapsed ? item.label : ''}
                                >
                                    <span className="flex-shrink-0">
                                        {item.icon}
                                    </span>
                                    {!isCollapsed && (
                                        <span className="text-sm font-medium">
                                            {item.label}
                                        </span>
                                    )}
                                </Link>
                            </li>
                        );
                    })}
                </ul>
            </nav>

            {/* Logout Button */}
            <div className="p-4">
                <button
                    onClick={handleLogout}
                    className={`
                        w-full flex items-center gap-3 bg-red-500 text-white px-4 py-3 rounded-lg 
                        hover:bg-red-600 active:bg-red-700 transition-all
                        ${isCollapsed ? 'justify-center' : ''}
                    `}
                    title={isCollapsed ? "Cerrar sesión" : ''}
                >
                    <MdLogout size={20} />
                    {!isCollapsed && <span className="text-sm font-medium">Cerrar sesión</span>}
                </button>
            </div>
        </aside>
    );
}

export default Sidebar;