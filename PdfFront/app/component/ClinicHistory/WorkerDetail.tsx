"use client";

import React from "react";
import { WorkerDto } from "@/types/api";

interface WorkerDetailProps {
    worker: WorkerDto;
}

const WorkerDetail: React.FC<WorkerDetailProps> = ({ worker }) => {
    const fields = [
        {
            label: "Nombre",
            value: worker.name,
        },
        {
            label: "Cédula",
            value: worker.cc,
        },
        {
            label: "Género",
            value: worker.gender,
        },
        {
            label: "Edad",
            value: worker.age,
        },
        {
            label: "Estado civil",
            value: worker.maritalStatus,
        },
        {
            label: "Nivel educativo",
            value: worker.educationalLevel,
        },
    ];

    return (
        <div className="rounded-2xl border border-gray-200 bg-white shadow-sm overflow-hidden">
            <div className="border-b border-gray-200 px-4 py-4 sm:px-6">
                <h2 className="text-lg font-semibold text-gray-900">
                    Información del trabajador
                </h2>
            </div>

            <div className="p-4 sm:p-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                    {fields.map((field) => (
                        <div key={field.label}>
                            <p className="text-xs font-medium uppercase tracking-wide text-gray-500">
                                {field.label}
                            </p>

                            <div className="mt-1 rounded-lg border border-gray-200 bg-gray-50 p-3 text-sm text-gray-900 break-words">
                                {field.value || "N/A"}
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default WorkerDetail;