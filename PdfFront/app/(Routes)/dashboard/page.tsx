"use client";

import HC from "@/component/UI/HC";
import type { DataHC, Worker, Exam } from "@/component/UI/HC/types";
import React from "react";

const HomePage = () => {
    return (
      <>
        <div>
          {/* Sección de contenido */}
          <div className="mt-8 min-h-screen bg-white rounded-lg shadow-md">
            <h1 className="pt-8 pl-14 text-3xl font-bold text-gray-800 mb-6">Dashboard</h1>
            <div className="space-y-6 px-6">
              {/* Información del Trabajador */}
              <div>
                <h2 className="text-2xl font-semibold text-gray-700 mb-4">Información del HC</h2>
              </div>
            </div>
          </div>
        </div>
      </>
    );
}

export default HomePage;