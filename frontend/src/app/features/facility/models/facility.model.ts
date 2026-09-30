export enum FacilityType {
  HOSPITAL = 'HOSPITAL',
  CLINIC = 'CLINIC',
  OTHER = 'OTHER'
}

export interface Facility {
  id: number;
  code: string;
  name: string;
  facilityType: FacilityType;
  address: string | null;
  phoneNumber: string | null;
  isActive: boolean;
}

export interface CreateFacilityRequest {
  code: string;
  name: string;
  facilityType: FacilityType;
  address: string | null;
  phoneNumber: string | null;
  isActive: boolean;
}

export interface UpdateFacilityRequest extends CreateFacilityRequest {
  id: number;
}
