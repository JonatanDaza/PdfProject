"use client";

import React from "react";
import { ClinicalHistoryDto, ExamDto, WorkerDto } from "@/types/api";
import { getWorkerFromExams, getUniqueExamNames } from "./utils";

interface ClinicHistoryCardProps {
    history: ClinicalHistoryDto;
    onOpen: (history: ClinicalHistoryDto) => void;
}

const ClinicHistoryCard: React.FC<ClinicHistoryCardProps> = ({ history, onOpen }) => {
    const worker: WorkerDto | null = getWorkerFromExams(history.exams ?? []);
    const examNames: string[] = getUniqueExamNames(history.exams ?? []);

    return (
        <article className="rounded-2xl border border-gray-200 bg-white p-5 shadow-sm hover:shadow-md transition-shadow">
            <div className="space-y-3">
                <div>
                    <p className="text-xs uppercase tracking-wide text-gray-500">Historia clínica</p>
                    <h2 className="text-lg font-semibold text-gray-900">#{history.id}</h2>
                </div>

                <div className="rounded-lg bg-gray-50 p-3 text-sm text-gray-700 space-y-1">
                    <p>
                        <span className="font-semibold">Trabajador:</span> {worker?.name ?? "No disponible"}
                    </p>
                    <p>
                        <span className="font-semibold">CC:</span> {worker?.CC ?? "-"}
                    </p>
                    <p>
                        <span className="font-semibold">Edad:</span> {worker?.Age ?? "-"}
                    </p>
                    <p>
                        <span className="font-semibold">Género:</span> {worker?.Gender ?? "-"}
                    </p>
                </div>

                <div>
                    <p className="text-sm font-semibold text-gray-800 mb-2">Exámenes</p>
                    <div className="flex flex-wrap gap-2">
                        {examNames.length ? (
                            examNames.map((name) => (
                                <span
                                    key={`${history.id}-${name}`}
                                    className="rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-700"
                                >
                                    {name}
                                </span>
                            ))
                        ) : (
                            <span className="text-sm text-gray-500">Sin exámenes listados</span>
                        )}
                    </div>
                </div>

                <button
                    type="button"
                    onClick={() => onOpen(history)}
                    className="w-full rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 transition-colors"
                >
                    Entrar a la historia
                </button>
            </div>
        </article>
    );
};

export default ClinicHistoryCard;
