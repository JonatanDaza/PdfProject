
// Tipos genéricos para DataTable
export interface TableColumn<T> {
    key: keyof T | string;
    header: string;
    className?: string;
    render?: (row: T) => React.ReactNode;
}

export interface TableProps<T> {
    columns: TableColumn<T>[];
    data: T[];
    isDarkMode?: boolean;
    pageSize?: number;
    filterPlaceholder?: string;
    filterFunction?: (row: T, filter: string) => boolean;
    className?: string;
    onRowClick?: (row: T) => void;
    onAddClick?: () => void;
    addButtonText?: string;
    addButtonClassName?: string;
    externalPage?: number;
    onExternalPageChange?: (page: number) => void;
    paginator?: React.FC<any>;
    externalTotalPages?: number;
}