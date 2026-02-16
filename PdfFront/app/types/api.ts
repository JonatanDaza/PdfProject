export interface PaginationRequest {
  page: number;
  size: number;
}

export interface PaginationResponse<T> {
  data: T[];
  code: string;
  message: string;
  totalPages: number;
  currentPage: number;
  totalElements: number;
}

export interface BaseEntity {
  id: string;
  [key: string]: any;
}

export type ClinicalHistoryDto = BaseEntity & {
  patientName: string;
  date: string;
  content: string;
};