export interface SourceSystemSearchParams {
  name?: string;
  code?: string;
  facilityId?: number;
  isActive?: boolean;
  page?: number;
  size?: number;
}

export const defaultSearchParams: SourceSystemSearchParams = {
  page: 0,
  size: 10
};
