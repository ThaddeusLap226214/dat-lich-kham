export interface PageResponse<T> {
  meta: PageMeta;
  result: T[];
}

export interface PageMeta {
  page: number;
  pageSize: number;
  pages: number;
  total: number;
}
