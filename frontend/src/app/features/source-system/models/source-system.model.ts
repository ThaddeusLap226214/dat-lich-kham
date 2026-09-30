export interface SourceSystem {
  id: number;
  facilityId: number;
  facilityName: string;
  facilityCode: string;
  code: string;
  name: string;
  description: string | null;
  isActive: boolean;
}

export interface CreateSourceSystemRequest {
  facilityId: number;
  code: string;
  name: string;
  description: string | null;
  isActive: boolean;
}

export interface UpdateSourceSystemRequest extends CreateSourceSystemRequest {
  id: number;
}
