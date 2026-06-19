"use client";

import React from "react";
import { useClinicHistories } from "./useClinicHistories";
import ClinicHistoryCard from "./ClinicHistoryCard";
import { StateCard, PaginationControls } from "./UIStates";

const ReadHCContainer: React.FC = () => {
    const { histories, loading, error, currentPage, totalPages, goToPage } = useClinicHistories();

    return (
        <div className="space-y-4 sm:space-y-6 p-6 bg-white rounded-lg shadow-md border border-gray-200">
            <div className="flex items-center justify-between flex-wrap gap-3">
                <h1 className="text-xl sm:text-2xl md:text-3xl font-bold text-gray-900">Historias clínicas agrupadas</h1>
            </div>

            {loading && <StateCard variant="loading" message="Cargando historias clínicas..." />}
            {error && <StateCard variant="error" message={error} />}
            {!loading && !error && histories.length === 0 && (
                <StateCard variant="empty" message="No hay historias clínicas para mostrar." />
            )}

            {histories.length > 0 && (
                <div className="grid gap-3 grid-cols-1 sm:grid-cols-2 lg:grid-cols-3">
                    {histories.map((history) => (
                        <ClinicHistoryCard key={history.id} history={history} />
                    ))}
                </div>
            )}

            {histories.length > 0 && (
                <PaginationControls
                    currentPage={currentPage}
                    totalPages={totalPages}
                    loading={loading}
                    onPrevious={() => goToPage(currentPage - 1)}
                    onNext={() => goToPage(currentPage + 1)}
                />
            )}
        </div>
    );
};

export default ReadHCContainer;