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
                            {/* <main className="mt-14 sm:mt-16 flex-1 p-4 sm:p-6 lg:p-8"> */}
                            <main className="p-3 pt-11">
                                {children}
                            </main>
                        </div>
                </div>
            </div>
        </ProtectedRoute>
    );
}

export default LayoutContent;