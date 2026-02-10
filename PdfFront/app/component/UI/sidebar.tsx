"use client";

import React from "react";
import Link from "next/link";

interface SidebarProps {
    isOpen: boolean;
    closeSidebar: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ isOpen, closeSidebar }) => {
    const menuItems = [
        { name: "Principal", href: "/" }
    ];

    return (
        <>
            {/* Overlay para cerrar sidebar en móvil */}
            {isOpen && (
                <div 
                    className="fixed inset-0 bg-transparent bg-opacity-50 z-30 lg:hidden"
                    onClick={closeSidebar}
                ></div>
            )}

            {/* Sidebar - Fijo en desktop, deslizable en móvil */}
            <aside 
                className={`
                    fixed top-0 left-0 bottom-0 w-64 bg-white shadow-lg z-40
                    transform transition-transform duration-300 ease-in-out
                    lg:translate-x-0
                    ${isOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
                `}
            >
                <nav className="p-4 pt-30">
                    <ul className="space-y-2">
                        {menuItems.map((item) => (
                            <li key={item.href}>
                                <Link 
                                    href={item.href}
                                    className="block px-4 py-3 text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                                    onClick={closeSidebar}
                                >
                                    {item.name}
                                </Link>
                            </li>
                        ))}
                    </ul>
                </nav>
            </aside>
        </>
    );
}

export default Sidebar;