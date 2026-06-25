"use client";

import React from "react";
import Link from "next/link";
import { useParams, usePathname } from "next/navigation";
import { useClinicHistoryDetail } from "@/component/ClinicHistory/useClinicHistories";
import ExamDetail from "@/component/ClinicHistory/ExamDetail";
import { StateCard } from "@/component/ClinicHistory/UIStates";
import { getWorkerFromExams } from "@/component/ClinicHistory/utils";
import WorkerDetail from "@/component/ClinicHistory/WorkerDetail";

const ReadHCDetailPage: React.FC = () => {
    const params = useParams<{ id: string }>();
    const id = Number(params.id);

    const { exams, loading, error } = useClinicHistoryDetail(id);
    const worker = getWorkerFromExams(exams);

    const pathname = usePathname();
    const backHref = pathname.split("/").slice(0, -1).join("/") || "/";

    return (
        <div className="w-full max-w-7xl mx-auto px-3 sm:px-4 lg:px-6 py-4">
            <section className="space-y-4 sm:space-y-6">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex-1 min-w-0">
                        <h1 className="text-lg sm:text-2xl md:text-3xl font-bold text-gray-900 break-words">
                            Historia clínica {id}
                        </h1>
                    </div>

                    <div className="flex justify-center sm:justify-end">
                        <Link
                            href={backHref}
                            className="w-full sm:w-auto text-center rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-100 transition-colors"
                        >
                            Volver a historias
                        </Link>
                    </div>
                </div>


                {worker && (
                    <WorkerDetail worker={worker} />
                )}

                {loading && (
                    <StateCard
                        variant="loading"
                        message="Cargando exámenes..."
                    />
                )}

                {error && (
                    <StateCard
                        variant="error"
                        message={error}
                    />
                )}

                {!loading && !error && exams.length === 0 && (
                    <StateCard
                        variant="empty"
                        message="Esta historia clínica no tiene exámenes registrados."
                    />
                )}

                <div className="grid grid-cols-1 gap-4 lg:gap-6">
                    {exams.map((exam) => (
                        <ExamDetail
                            key={exam.id}
                            exam={exam}
                        />
                    ))}
                </div>
            </section>
        </div>
    );
};

export default ReadHCDetailPage;