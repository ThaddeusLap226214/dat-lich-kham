import { FacilityType } from './facility.model';

export interface FacilitySearchParams {
  name?: string;
  code?: string;
  facilityType?: FacilityType;
  isActive?: boolean;
  page?: number;
  size?: number;
}

export const defaultSearchParams: FacilitySearchParams = {
  page: 0,
  size: 10
};
