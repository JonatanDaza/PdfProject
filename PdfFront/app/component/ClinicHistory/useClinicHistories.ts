"use client";

import { useCallback, useEffect, useState } from "react";
import { ClinicalHistoryDto, ExamDto } from "@/types/api";
import { getAllHC, getExamsByClinicalHistoryId } from "@/services/api/processPdfService";

const PAGE_SIZE = 9;

interface UseClinicHistoriesState {
    histories: ClinicalHistoryDto[];
    loading: boolean;
    error: string | null;
    currentPage: number;
    totalPages: number;
    selectedHistory: ClinicalHistoryDto | null;
    detailLoading: boolean;
    detailError: string | null;
}

export const useClinicHistories = () => {
    const [state, setState] = useState<UseClinicHistoriesState>({
        histories: [],
        loading: true,
        error: null,
        currentPage: 0,
        totalPages: 0,
        selectedHistory: null,
        detailLoading: false,
        detailError: null,
    });

    const loadHistories = useCallback(async (page: number) => {
        setState((prev) => ({ ...prev, loading: true, error: null }));
        try {
            const response = await getAllHC<ClinicalHistoryDto>(page, PAGE_SIZE);
            setState((prev) => ({
                ...prev,
                histories: response.data ?? [],
                totalPages: response.totalPages ?? 0,
                currentPage: response.currentPage ?? page,
                loading: false,
            }));
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "No se pudieron cargar las historias clínicas.";
            setState((prev) => ({ ...prev, error: message, loading: false }));
        }
    }, []);

    const openHistoryDetail = useCallback(async (history: ClinicalHistoryDto) => {
        setState((prev) => ({ ...prev, detailLoading: true, detailError: null }));
        try {
            const examsResponse = await getExamsByClinicalHistoryId(Number(history.id));
            setState((prev) => ({
                ...prev,
                selectedHistory: {
                    ...history,
                    exams: examsResponse.data ?? [],
                },
                detailLoading: false,
            }));
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "No se pudieron cargar los exámenes.";
            setState((prev) => ({
                ...prev,
                detailError: message,
                selectedHistory: history,
                detailLoading: false,
            }));
        }
    }, []);

    const closeHistoryDetail = useCallback(() => {
        setState((prev) => ({
            ...prev,
            selectedHistory: null,
            detailError: null,
        }));
    }, []);

    const goToPage = useCallback(
        (page: number) => {
            const nextPage = Math.max(0, Math.min(page, state.totalPages - 1));
            loadHistories(nextPage);
        },
        [state.totalPages, loadHistories]
    );

    useEffect(() => {
        loadHistories(0);
    }, [loadHistories]);

    return {
        ...state,
        loadHistories,
        openHistoryDetail,
        closeHistoryDetail,
        goToPage,
    };
};
