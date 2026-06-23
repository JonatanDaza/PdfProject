"use client";

import React from "react";
import { ExamDto } from "@/types/api";
import JsonValueRenderer from "./JsonValueRenderer";

interface ExamDetailProps {
    exam: ExamDto;
}

const ExamDetail: React.FC<ExamDetailProps> = ({ exam }) => {
    return (
        <div className="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-sm">
            {/* Header */}
            <div className="border-b border-gray-200 px-4 py-4 sm:px-6">
                <h2 className="break-words text-lg font-semibold text-gray-900">
                    {exam.examName}
                </h2>

                <p className="mt-1 text-sm text-gray-600">
                    Fecha: {exam.examDate || "Sin fecha"}
                </p>
            </div>

            {/* Content */}
            <div className="p-4 sm:p-6">
                {exam.dataValues?.length ? (
                    <div className="flex flex-wrap gap-4">
                        {exam.dataValues.map((item) => {
                            const fieldName =
                                item.dataType?.name?.toLowerCase() || "";

                            const isObservation =
                                fieldName.includes("observ");

                            const isRequirementList =
                                fieldName.includes("requisito");

                            const isRequirementConduct =
                                fieldName.includes("conduct");

                            const isAudiometry =
                                fieldName.includes("audiometr");

                            const isRecomendation =
                                fieldName.includes("recomend");

                            const isLargeField =
                                isObservation || isAudiometry || isRequirementList || isRequirementConduct;

                            const isMediumField =
                                !isLargeField &&
                                (fieldName.includes("resultado") || fieldName.includes("comentario"));

                            return (
                                <div
                                    key={item.id}
                                    className={
                                        isLargeField
                                            ? "w-full"
                                            : isMediumField
                                            ? "w-full lg:basis-[48%] lg:flex-1"
                                            : "w-full sm:basis-[48%] xl:basis-[31%] flex-1"
                                    }
                                >
                                    <label className="mb-1 block break-words text-xs font-medium uppercase tracking-wide text-gray-500">
                                        {item.dataType?.name ?? "Sin tipo"}
                                    </label>

                                    <div className="rounded-lg border border-gray-200 bg-gray-50 p-3">
                                        <div className="overflow-x-auto">
                                            <div className="break-words whitespace-pre-wrap text-sm text-gray-800">
                                                <JsonValueRenderer
                                                    value={String(item.value)}
                                                />
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                ) : (
                    <div className="rounded-lg bg-gray-50 p-4 text-sm text-gray-600">
                        Este examen no tiene valores registrados.
                    </div>
                )}
            </div>
        </div>
    );
};

export default ExamDetail;