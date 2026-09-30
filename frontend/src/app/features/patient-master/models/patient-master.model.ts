import { Gender } from '../../../shared/enums';
import { PatientMasterStatusEnum } from '../models/patient-master.constants'

export interface PatientMaster {
  id: number;
  enterpriseId: string; // EMPI
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  nationalId?: string;
  healthInsuranceNo?: string;
  phoneNumber?: string;
  address?: string;
  status: PatientMasterStatusEnum; // ACTIVE, INACTIVE, MERGED
  linkedPatientCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface PatientMasterSearchRequest {
  keyword?: string;
  gender?: Gender;
  status?: string;
  ageFrom?: number;
  ageTo?: number;
  sourceSystemIds?: number[];
  page: number;
  size: number;
}
export const defaultSearchParams: PatientMasterSearchRequest = {
  page: 0,
  size: 10
};