"use client";

import { useState } from "react";
import Header from "@/component/UI/header";
import Sidebar from "@/component/UI/sidebar";

interface LayoutContentProps {
    children: React.ReactNode;
}

const LayoutContent: React.FC<LayoutContentProps> = ({ children }) => {
    const [isSidebarOpen, setIsSidebarOpen] = useState(false);

    const toggleSidebar = () => {
        setIsSidebarOpen(!isSidebarOpen);
    };

    return (
        <div className="min-h-screen bg-gray-50">
            {/* Header fijo */}
            <Header toggleSidebar={toggleSidebar} />
            
            {/* Sidebar */}
            <Sidebar isOpen={isSidebarOpen} closeSidebar={() => setIsSidebarOpen(false)} />
            
            {/* Contenido principal con margen izquierdo para el sidebar */}
            <main className="pt-16 p-6 lg:ml-64">
                {children}
            </main>
        </div>
    );
}

export default LayoutContent;