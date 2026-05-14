"use client";

import React from "react";
import { useClinicHistories } from "./useClinicHistories";
import ClinicHistoryCard from "./ClinicHistoryCard";
import ExamDetail from "./ExamDetail";
import { StateCard, PaginationControls } from "./UIStates";
import { getWorkerFromExams } from "./utils";

const ReadHCContainer = () => {
    const {
        histories,
        loading,
        error,
        currentPage,
        totalPages,
        selectedHistory,
        detailLoading,
        detailError,
        openHistoryDetail,
        closeHistoryDetail,
        goToPage,
    } = useClinicHistories();

    const workerInDetail = selectedHistory ? getWorkerFromExams(selectedHistory.exams) : null;

    // Vista de detalle de historia
    if (selectedHistory) {
        return (
            <section className="space-y-6">
                <div className="flex items-center justify-between flex-wrap gap-3">
                    <div>
                        <h1 className="text-2xl font-bold text-gray-900">
                            Historia clínica #{selectedHistory.id}
                        </h1>
                        {workerInDetail && (
                            <p className="text-sm text-gray-600 mt-1">
                                Trabajador: <span className="font-semibold">{workerInDetail.name}</span> |
                                CC: {workerInDetail.CC}
                            </p>
                        )}
                    </div>
                    <button
                        type="button"
                        onClick={closeHistoryDetail}
                        className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                    >
                        Volver a historias
                    </button>
                </div>

                {detailLoading && <StateCard variant="loading" message="Cargando exámenes..." />}
                {detailError && <StateCard variant="error" message={detailError} />}
                {!detailLoading && selectedHistory.exams.length === 0 && (
                    <StateCard variant="empty" message="Esta historia clínica no tiene exámenes registrados." />
                )}

                <div className="grid gap-4">
                    {selectedHistory.exams.map((exam) => (
                        <ExamDetail key={exam.id} exam={exam} />
                    ))}
                </div>
            </section>
        );
    }

    // Vista de listado de historias
    return (
        <section className="space-y-6">
            <div className="flex items-center justify-between flex-wrap gap-3">
                <h1 className="text-2xl font-bold text-gray-900">Historias clínicas agrupadas</h1>
            </div>

            {loading && <StateCard variant="loading" message="Cargando historias clínicas..." />}
            {error && <StateCard variant="error" message={error} />}
            {!loading && !error && histories.length === 0 && (
                <StateCard variant="empty" message="No hay historias clínicas para mostrar." />
            )}

            {histories.length > 0 && (
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                    {histories.map((history) => (
                        <ClinicHistoryCard
                            key={history.id}
                            history={history}
                            onOpen={openHistoryDetail}
                        />
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
        </section>
    );
};

export default ReadHCContainer;