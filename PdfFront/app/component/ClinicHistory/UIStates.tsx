"use client";

import React from "react";

interface StateCardProps {
    variant: "loading" | "error" | "empty";
    message: string;
}

export const StateCard: React.FC<StateCardProps> = ({ variant, message }) => {
    const classNames: Record<string, string> = {
        loading: "rounded-xl border border-blue-200 bg-blue-50 p-4 text-blue-700 text-sm",
        error: "rounded-xl border border-red-200 bg-red-50 p-4 text-red-700 text-sm",
        empty: "rounded-xl border border-gray-200 bg-white p-6 text-center text-gray-600",
    };

    return <div className={classNames[variant]}>{message}</div>;
};

interface PaginationControlsProps {
    currentPage: number;
    totalPages: number;
    loading: boolean;
    onPrevious: () => void;
    onNext: () => void;
}

export const PaginationControls: React.FC<PaginationControlsProps> = ({
    currentPage,
    totalPages,
    loading,
    onPrevious,
    onNext,
}) => {
    return (
        <div className="flex items-center justify-between pt-2">
            <button
                type="button"
                onClick={onPrevious}
                disabled={currentPage <= 0 || loading}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50 transition-colors"
            >
                Anterior
            </button>

            <p className="text-sm text-gray-600">
                Página <span className="font-semibold">{currentPage + 1}</span> de{" "}
                <span className="font-semibold">{Math.max(totalPages, 1)}</span>
            </p>

            <button
                type="button"
                onClick={onNext}
                disabled={loading || currentPage >= totalPages - 1}
                className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 disabled:opacity-50 transition-colors"
            >
                Siguiente
            </button>
        </div>
    );
};
