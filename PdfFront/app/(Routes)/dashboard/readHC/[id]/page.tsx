"use client";

import React from "react";
import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import { useClinicHistoryDetail } from "@/component/ClinicHistory/useClinicHistories";
import ExamDetail from "@/component/ClinicHistory/ExamDetail";
import { StateCard } from "@/component/ClinicHistory/UIStates";
import { getWorkerFromExams } from "@/component/ClinicHistory/utils";

const ReadHCDetailPage: React.FC = () => {
    const params = useParams<{ id: string }>();
    const id = Number(params.id);
    const { exams, loading, error } = useClinicHistoryDetail(id);
    const worker = getWorkerFromExams(exams);

    const pathname = usePathname();
    const backHref = pathname.split("/").slice(0, -1).join("/") || "/";

    return (
        <div className="min-w-full max-w-full mx-auto px-3 sm:px-3 lg:px-3 pt-3 overflow-x-hidden">
            <section className="space-y-4 sm:space-y-6">
                <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                    <div className="flex-1 min-w-0">
                        <h1 className="text-xl sm:text-2xl md:text-3xl font-bold text-gray-900 break-words">
                            Historia clínica #{id}
                        </h1>
                        <p className="text-xs sm:text-sm text-gray-600 mt-2 break-words">
                            <span className="block sm:inline">Trabajador: <span className="font-semibold">{worker?.name || 'N/A'}</span></span>
                            <span className="hidden sm:inline"> | </span>
                            <span className="block sm:inline">CC: {worker?.cc || 'N/A'}</span>
                        </p>
                    </div>
                    <Link
                        href={backHref}
                        className="w-full sm:w-auto shrink-0 text-center rounded-lg border border-gray-300 px-4 py-2 text-xs sm:text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                    >
                        Volver a historias
                    </Link>
                </div>

                {loading && <StateCard variant="loading" message="Cargando exámenes..." />}
                {error && <StateCard variant="error" message={error} />}
                {!loading && !error && exams.length === 0 && (
                    <StateCard variant="empty" message="Esta historia clínica no tiene exámenes registrados." />
                )}

                <div className="grid grid-cols-1 gap-4 w-full">
                    {exams.map((exam) => (
                        <ExamDetail key={exam.id} exam={exam} />
                    ))}
                </div>
            </section>
        </div>
    );
};

export default ReadHCDetailPage;