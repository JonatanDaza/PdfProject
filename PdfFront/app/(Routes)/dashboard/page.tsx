"use client";

import React, { useState } from "react";
import { MdShield } from "react-icons/md";
import Image from "next/image";


const HomePage = () => {
    return (
      <div className="min-h-screen space-y-12 animate-in fade-in duration-700">
        
        {/* Hero Section */}
        <div className="bg-white rounded-[2rem] shadow-sm border border-gray-100 overflow-hidden relative">
          <div className="flex flex-col lg:flex-row items-center gap-12 p-8 lg:p-16">
            <div className="flex-1 space-y-8 relative z-10">
              <div className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-50 rounded-full text-indigo-600 text-xs font-black uppercase tracking-widest border border-indigo-100">
                <MdShield size={14} />
                Sistema Seguro & Centralizado
              </div>
              <h1 className="text-4xl lg:text-6xl font-black text-gray-900 leading-[1.1] text-center">
                Gestión de Historias Clínicas
              </h1>
              <p className="text-lg text-gray-500 leading-relaxed max-w-xl">
                Plataforma de alta seguridad para la extracción y gestión centralizada de historias clínicas. 
                Optimice la toma de decisiones con métricas en tiempo real y flujos automatizados.
              </p>
            </div>

            <div className="flex-1 relative w-full aspect-square max-w-[500px]">
              <div className="absolute inset-0 bg-indigo-600/5 rounded-full blur-3xl animate-pulse" />
              <div className="relative h-full w-full h-[300px] md:h-[450px]">
                <Image
                  src="/img/doctor-home.jpg"
                  alt="DIAN Health Professional"
                  fill
                  sizes="(max-width: 768px) 100vw, 50vw"
                  className="object-cover rounded-[3rem] shadow-2xl rotate-2 hover:rotate-0 transition-transform duration-700"
                  priority
                />
              </div>
            </div>
          </div>

          {/* Abstract backgrounds */}
          <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-50 rounded-full -mr-32 -mt-32 blur-3xl opacity-50" />
          <div className="absolute bottom-0 left-0 w-64 h-64 bg-emerald-50 rounded-full -ml-32 -mb-32 blur-3xl opacity-50" />
        </div>
        
      </div>
    );
}

export default HomePage;