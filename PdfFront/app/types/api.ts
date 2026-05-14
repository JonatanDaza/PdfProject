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

export interface ListResponse<T> {
  data: T[];
  code: string;
  message: string;
  totalElements: number;
}

export interface BaseEntity {
  id: string | number;
  [key: string]: any;
}

export interface DataTypeDto {
  id: number;
  name: string;
}

export interface DataValueDto {
  id: number;
  value: unknown;
  dataType: DataTypeDto;
}

export interface WorkerDto {
  id: number;
  CC: string;
  name: string;
  Gender: string;
  Age: string;
  MaritalStatus: string;
  EducationalLevel: string;
}

export interface ExamDto {
  id: number;
  examName: string;
  examDate: string;
  clinicalHistoryId: number;
  dataValues: DataValueDto[];
  worker: WorkerDto | null;
}

export type ClinicalHistoryDto = BaseEntity & {
  id: number;
  exams: ExamDto[];
};