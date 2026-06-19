"use client";

import React from "react";
import { ExamDto } from "@/types/api";
import { formatDataValue } from "./utils";

interface ExamDetailProps {
    exam: ExamDto;
}

const ExamDetail: React.FC<ExamDetailProps> = ({ exam }) => {
    return (
        <div className="w-[100%] rounded-2xl border border-gray-200 bg-white p-3 sm:p-5 shadow-sm">
            <div className="mb-3 sm:mb-4">
                <h2 className="text-base sm:text-lg font-semibold text-gray-900">{exam.examName}</h2>
                <p className="text-xs sm:text-sm text-gray-500">Fecha: {exam.examDate || "Sin fecha"}</p>
            </div>

            {exam.dataValues?.length ? (
                <div className="overflow-x-auto -mx-3 sm:-mx-5 px-3 sm:px-5">
                    <table className="w-full text-xs sm:text-sm">
                        {/* <thead>
                            <tr className="border-b border-gray-200 text-left text-gray-600">
                                <th className="py-2 pr-4 font-semibold">dataType</th>
                                <th className="py-2 font-semibold">dataValue</th>
                            </tr>
                        </thead> */}
                        <tbody>
                            {exam.dataValues.map((item) => (
                                <tr key={item.id} className="border-b border-gray-100 align-top">
                                    <td className="py-2 pr-2 sm:pr-4 font-medium text-gray-800 whitespace-normal sm:whitespace-nowrap text-xs sm:text-sm">
                                        {item.dataType?.name ?? "Sin tipo"}
                                    </td>
                                    <td className="py-2 text-gray-700">
                                        <pre className="whitespace-pre-wrap break-words font-sans text-xs sm:text-sm m-0">
                                            {formatDataValue(item.value)}
                                        </pre>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            ) : (
                <div className="rounded-lg bg-gray-50 p-3 text-xs sm:text-sm text-gray-600">
                    Este examen no tiene valores (`dataValue`) registrados.
                </div>
            )}
        </div>
    );
};

export default ExamDetail;
