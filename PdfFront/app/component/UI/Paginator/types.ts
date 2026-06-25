// Tipos para paginador
export interface PaginationProps {
    page: number;
    totalPages: number;
    onPageChange: (page: number) => void;
    isDarkMode?: boolean;
}