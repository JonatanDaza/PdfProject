// Modal.tsx — agrega ReactDOM.createPortal

import React, { useEffect } from "react";
import ReactDOM from "react-dom";
import { X } from "lucide-react";
import { ModalProps } from "./types";

const sizeMap = {
  sm:   "max-w-sm",
  md:   "max-w-md",
  lg:   "max-w-lg",
  xl:   "max-w-xl",
  xxl:  "max-w-2xl",
  xxxl: "max-w-3xl",
  full: "max-w-6xl",
};

const Modal: React.FC<ModalProps> = ({
  isOpen, onClose, title, size = "md", children, className = "",
}) => {
  const [isDarkMode, setIsDarkMode] = React.useState(false);

  React.useEffect(() => {
    const checkDarkMode = () =>
      setIsDarkMode(document.documentElement.classList.contains("dark"));
    checkDarkMode();
    const observer = new MutationObserver(checkDarkMode);
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ["class"] });
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    if (!isOpen) return;
    const onKeyDown = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKeyDown);
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      window.removeEventListener("keydown", onKeyDown);
      document.body.style.overflow = originalOverflow;
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const modalSize = sizeMap[size] || "max-w-lg";

  // ← Portal: monta directamente en document.body, fuera del árbol del layout
  return ReactDOM.createPortal(
    <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
      <div
        className={`
          w-full transform rounded-3xl shadow-2xl transition-all duration-300
          ${isDarkMode
            ? "border border-slate-700/50 bg-gradient-to-br from-slate-800 via-slate-800 to-slate-900 text-slate-100"
            : "border border-gray-200/80 bg-gradient-to-br from-white via-white to-gray-50 text-gray-800"
          }
          ${modalSize}
          animate-in slide-in-from-bottom-4 fade-in duration-300
          ${className}
        `}
      >
        {/* Header */}
        <div className={`flex items-center justify-between p-6 pb-4 ${
          title ? "border-b border-opacity-20" : ""
        } ${isDarkMode ? "border-slate-700/50" : "border-gray-200/60"}`}>
          {title && (
            <div className="flex items-center space-x-3">
              <div className={`h-8 w-1 rounded-full ${
                isDarkMode
                  ? ""
                  : ""
              }`} />
              <h2 className={`text-xl font-bold tracking-tight ${
                isDarkMode ? "text-slate-100" : "text-gray-900"
              }`}>
                {title}
              </h2>
            </div>
          )}
          <button
            onClick={onClose}
            className={`group relative rounded-xl p-2.5 ml-4 transition-all duration-200 ${
              isDarkMode
                ? "hover:bg-slate-700/70 active:bg-slate-600/80"
                : "hover:bg-gray-100/80 active:bg-gray-200/60"
            } hover:scale-105 active:scale-95`}
            aria-label="Cerrar modal"
          >
            <X className={`h-5 w-5 transition-all duration-200 group-hover:rotate-90 ${
              isDarkMode ? "text-slate-400 group-hover:text-slate-200" : "text-gray-500 group-hover:text-gray-700"
            }`} />
          </button>
        </div>

        {/* Contenido */}
        <div className={`px-6 pb-6 ${title ? "pt-4" : "pt-2"}`}>
          {children}
        </div>
      </div>
    </div>,
    document.body  // ← monta en body, completamente fuera del sidebar/header
  );
};

export default Modal;