"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { ClinicalHistoryDto, WorkerDto } from "@/types/api";
import { getWorkerFromExams, getUniqueExamNames } from "./utils";

interface ClinicHistoryCardProps {
    history: ClinicalHistoryDto;
}

const ClinicHistoryCard: React.FC<ClinicHistoryCardProps> = ({ history }) => {
    const pathname = usePathname();
    const worker: WorkerDto | null = getWorkerFromExams(history.exams ?? []);
    const examNames: string[] = getUniqueExamNames(history.exams ?? []);

    return (
        <div className="w-full max-w-full h-full flex flex-col rounded-2xl border border-gray-200 bg-gray-100 p-3 sm:p-5 shadow-sm hover:shadow-md transition-shadow overflow-hidden">
            <div className="flex-1">
                <div className="min-w-0">
                    <p className="text-lg sm:text-xl font-semibold text-gray-900 truncate">Historia clínica {history.id}</p>
                </div>

                <div className="rounded-lg p-2 sm:p-3 text-xs sm:text-sm text-gray-700 space-y-1 min-w-0">
                    <p className="break-words"><span className="font-semibold">Trabajador:</span> {worker?.name ?? "No disponible"}</p>
                    <p className="break-words"><span className="font-semibold">CC:</span> {worker?.cc ?? "-"}</p>
                    <p><span className="font-semibold">Edad:</span> {worker?.age ?? "-"}</p>
                    <p><span className="font-semibold">Género:</span> {worker?.gender ?? "-"}</p>
                </div>

                <div className="min-w-0">
                    <p className="text-xs sm:text-sm font-semibold text-gray-800 mb-2">Exámenes</p>
                    <div className="flex flex-wrap gap-1 sm:gap-2">
                        {examNames.length ? (
                            examNames.map((name) => (
                                <span
                                    key={`${history.id}-${name}`}
                                    className="max-w-full rounded-full bg-blue-100 px-2 sm:px-3 py-1 text-xs font-medium text-blue-700 truncate"
                                    title={name}
                                >
                                    {name}
                                </span>
                            ))
                        ) : (
                            <span className="text-xs sm:text-sm text-gray-500">Sin exámenes listados</span>
                        )}
                    </div>
                </div>
            </div>

            <div className="pt-3">
                <Link
                    href={`${pathname}/${history.id}`}
                    className="block w-full text-center rounded-lg bg-blue-600 px-4 py-2 text-xs sm:text-sm font-semibold text-white hover:bg-blue-700 active:bg-blue-800 transition-colors"
                >
                    Entrar a la historia
                </Link>
            </div>
        </div>
    );
};

export default ClinicHistoryCard;