/**
 * API Response types
 */

export interface ApiResponse<T = any> {
  data: T;
  timestamp: string;
}

export interface ApiError {
  error: string;
  message: string;
  status: number;
  timestamp: string;
  path: string;
  validationErrors?: Record<string, string[]>;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

export interface PaginationParams {
  page?: number;
  size?: number;
  sort?: string;
}
