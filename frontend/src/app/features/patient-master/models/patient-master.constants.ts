export enum PatientMasterStatusEnum {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  MERGED = 'MERGED',
  DUPLICATE = 'DUPLICATE'
}

export const PATIENT_STATUS_OPTIONS = [
  { value: PatientMasterStatusEnum.ACTIVE, label: 'Đang hoạt động' },
  { value: PatientMasterStatusEnum.INACTIVE, label: 'Không hoạt động' },
  { value: PatientMasterStatusEnum.MERGED, label: 'Đã gộp' },
  { value: PatientMasterStatusEnum.DUPLICATE, label: 'Trùng lặp' }
];
