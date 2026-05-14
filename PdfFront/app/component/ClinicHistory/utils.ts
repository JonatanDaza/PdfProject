import { DataValueDto, ExamDto, WorkerDto } from "@/types/api";

/**
 * Extrae el primer worker encontrado en la lista de exámenes
 */
export const getWorkerFromExams = (exams: ExamDto[]): WorkerDto | null => {
    return exams.find((exam) => exam.worker)?.worker ?? null;
};

/**
 * Formatea el valor de un dataValue para mostrar en UI
 */
export const formatDataValue = (value: DataValueDto["value"]): string => {
    if (value === null || value === undefined) return "-";
    if (typeof value === "string") return value;
    if (typeof value === "number" || typeof value === "boolean") return String(value);

    try {
        return JSON.stringify(value, null, 2);
    } catch {
        return String(value);
    }
};

/**
 * Extrae nombres únicos de exámenes de una lista
 */
export const getUniqueExamNames = (exams: ExamDto[]): string[] => {
    return Array.from(new Set(exams.map((exam) => exam.examName).filter(Boolean)));
};
