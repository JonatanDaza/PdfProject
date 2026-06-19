"use client";

import { useState } from "react";
import Header from "@/component/UI/header";
import Sidebar from "@/component/UI/sidebar";
import ProtectedRoute from "@/component/auth/ProtectedRoute";

interface LayoutContentProps {
    children: React.ReactNode;
}

const LayoutContent: React.FC<LayoutContentProps> = ({ children }) => {
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    return (
        <ProtectedRoute>
            <div className="flex min-h-screen bg-gray-50">
                {/* Sidebar */}
                <Sidebar 
                    onCollapsedChange={setSidebarCollapsed}
                />
                
                {/* Main Content Area */}
                <div
                    className={`
                        flex flex-col flex-1 w-full
                        transition-all duration-300
                        ${sidebarCollapsed ? 'ml-20' : 'ml-64'}
                    `}
                >
                    {/* Header */}
                    <Header
                        sidebarCollapsed={sidebarCollapsed}
                    />
                        {/* Contenido principal */}
                        <div>
                            <main className="p-3 pt-16 sm:pt-19">
                                {children}
                            </main>
                        </div>
                </div>
            </div>
        </ProtectedRoute>
    );
}

export default LayoutContent;