export interface DataHC<T> {
    key: keyof T | string;
    field: string;
    className?: string;
    render?: (row: T) => React.ReactNode;
}

export interface HCProps<T> {
    columns: DataHC<T>[];
    row: T;
    isDarkMode?: boolean;
    className?: string;
}

// Modelos basados en el backend
export interface Worker<T> {
    field: number;
    value: string;
}

export interface DataType<T> {
    field: number;
    value: string;
}

export interface DataValue<T> {
    field: number;
    value: string;
}

export interface Exam<T> {
    field: number;
    value: string;
}

export interface ClinicalHistory<T> {
    field: number;
    value: string;
}