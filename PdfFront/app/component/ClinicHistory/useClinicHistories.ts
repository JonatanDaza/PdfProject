"use client";

import { useCallback, useEffect, useState } from "react";
import { ClinicalHistoryDto, ExamDto } from "@/types/api";
import { getAllHC, getExamsByClinicalHistoryId } from "@/services/api/processPdfService";

const PAGE_SIZE = 10;

interface UseClinicHistoriesState {
    histories: ClinicalHistoryDto[];
    loading: boolean;
    error: string | null;
    currentPage: number;
    totalPages: number;
}

export const useClinicHistories = () => {
    const [state, setState] = useState<UseClinicHistoriesState>({
        histories: [],
        loading: true,
        error: null,
        currentPage: 0,
        totalPages: 0,
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
        goToPage,
    };
};

interface UseClinicHistoryDetailState {
    exams: ExamDto[];
    loading: boolean;
    error: string | null;
}

export const useClinicHistoryDetail = (id: number) => {
    const [state, setState] = useState<UseClinicHistoryDetailState>({
        exams: [],
        loading: true,
        error: null,
    });

    const loadDetail = useCallback(async () => {
        setState((prev) => ({ ...prev, loading: true, error: null }));
        try {
            const examsResponse = await getExamsByClinicalHistoryId(id);
            setState({
                exams: examsResponse.data ?? [],
                loading: false,
                error: null,
            });
        } catch (err) {
            const message =
                err instanceof Error ? err.message : "No se pudieron cargar los exámenes.";
            setState({ exams: [], loading: false, error: message });
        }
    }, [id]);

    useEffect(() => {
        loadDetail();
    }, [loadDetail]);

    return { ...state, reload: loadDetail };
};