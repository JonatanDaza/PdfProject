import api from "./api";

export const processPdf = async <T>(endpoint: string): Promise<T[]> => {
    const { data } = await api.get<T[]>(endpoint);
    return data;
}