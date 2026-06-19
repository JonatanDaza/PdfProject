import { API } from "./api";
import {
    PaginationResponse,
    ListResponse,
    BaseEntity,
    ClinicalHistoryDto,
    ExamDto,
} from "@/types/api";

export const extractHC = async <T extends BaseEntity = ClinicalHistoryDto>(file: File): Promise<T> => {
    const formData = new FormData();
    formData.append('file', file);
    
    const { data } = await API.api.post<T>('/HC/extract', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        }
    });
    return data;
}

export const getAllHC = async <T extends BaseEntity = ClinicalHistoryDto>(
    page: number = 0,
    size: number = 10
): Promise<PaginationResponse<T>> => {
    const { data } = await API.api.post<PaginationResponse<T>>('/HC/all', {
        page,
        size
    }, {
        headers: {
            'Content-Type': 'application/json',
        }
    });
    return data;
}

export const uploadHC = async (file: File): Promise<string> => {
    const formData = new FormData();
    formData.append('file', file);
    
    const { data } = await API.api.post<string>('/HC/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        }
    });
    return data;
}

export const getExamsByClinicalHistoryId = async (
    clinicalHistoryId: number
): Promise<ListResponse<ExamDto>> => {
    const { data } = await API.api.get<ListResponse<ExamDto>>(`/HC/${clinicalHistoryId}/exams`);
    return data;
}