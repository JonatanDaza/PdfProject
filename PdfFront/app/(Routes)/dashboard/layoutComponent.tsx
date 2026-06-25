"use client";

import { useState } from "react";
import Header from "@/component/UI/header";
import Sidebar from "@/component/UI/sidebar";
import ProtectedRoute from "@/component/auth/ProtectedRoute";
import { Toaster } from "sonner";

interface LayoutContentProps {
    children: React.ReactNode;
}

const LayoutContent: React.FC<LayoutContentProps> = ({ children }) => {
    const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

    return (
        <ProtectedRoute requiredRoles="admin">
            <Toaster position="top-right" richColors />
            <div className="flex min-h-screen bg-gray-50">
                {/* <Sidebar onCollapsedChange={setSidebarCollapsed} /> */}
                {/* <div className={`flex flex-col flex-1 min-w-0 transition-all duration-300 ${sidebarCollapsed ? 'ml-20' : 'ml-60'}`}> */}
                    <Header sidebarCollapsed={sidebarCollapsed} />
                    <main className="flex-1 mt-14 sm:mt-12 p-4 sm:p-6 overflow-y-auto">
                        {children}
                    </main>
                {/* </div> */}
            </div>
        </ProtectedRoute>
    );
}

export default LayoutContent;