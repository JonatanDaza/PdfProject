import { API } from "./api";
import { PaginationResponse, BaseEntity, ClinicalHistoryDto } from "@/types/api";

export const extractHC = async <T extends BaseEntity = ClinicalHistoryDto>(file: File): Promise<T> => {
    const formData = new FormData();
    formData.append('file', file);
    
    const { data } = await API.api.post<T>('/extract', formData, {
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
    const { data } = await API.api.post<PaginationResponse<T>>('/all', {
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
    
    const { data } = await API.api.post<string>('/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        }
    });
    return data;
}