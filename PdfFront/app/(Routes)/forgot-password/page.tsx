"use client";

import React from "react";
import Link from "next/link";
import { 
  MdSupportAgent, 
  MdShield,
  MdArrowBack
} from "react-icons/md";

const ForgotPassword = () => {
    return (
      <div className="min-h-screen space-y-12 pb-20 animate-in fade-in duration-700">

        {/* Hero Section */}
        <div className="bg-white rounded-[2rem] shadow-sm border border-gray-100 overflow-hidden relative">

          {/* Back link - top right */}
          <Link 
            href="/login" 
            className="absolute top-6 right-6 z-20 inline-flex items-center gap-2 px-4 py-2 bg-gray-50 hover:bg-gray-100 rounded-full text-sm font-bold text-gray-500 hover:text-indigo-600 transition-colors"
          >
            <MdArrowBack />
            Volver al inicio de sesión
          </Link>

          <div className="flex flex-col items-center gap-8 p-8 lg:p-16 text-center max-w-2xl mx-auto relative z-10">

            <div className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-50 rounded-full text-indigo-600 text-xs font-black uppercase tracking-widest border border-indigo-100">
              <MdShield size={14} />
              Sistema Seguro & Centralizado
            </div>

            <div className="w-20 h-20 bg-indigo-600 rounded-3xl flex items-center justify-center text-white shadow-xl shadow-indigo-100">
              <MdSupportAgent size={36} />
            </div>

            <h1 className="text-3xl lg:text-5xl font-black text-gray-900 leading-[1.1]">
              ¿Olvidaste tu contraseña?
            </h1>

            <p className="text-lg text-gray-500 leading-relaxed">
              Por tu seguridad, el cambio de contraseña no se realiza de forma automática.
              Comunícate con nuestro equipo de soporte técnico escribiendo a{" "}
              <a 
                href="mailto:desarrollo@zonamedicaips.com" 
                className="text-indigo-600 font-bold hover:underline"
              >
                desarrollo@zonamedicaips.com
              </a>{" "}
              y con gusto te ayudaremos a recuperar el acceso a tu cuenta.
            </p>
          </div>

          {/* Abstract backgrounds */}
          <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-50 rounded-full -mr-32 -mt-32 blur-3xl opacity-50" />
          <div className="absolute bottom-0 left-0 w-64 h-64 bg-emerald-50 rounded-full -ml-32 -mb-32 blur-3xl opacity-50" />
        </div>
      </div>
    );
}

export default ForgotPassword;