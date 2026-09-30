import { IdType, PatientStatus } from './patient.model';

export const STATUS_OPTIONS = [
  { value: PatientStatus.TEMPORARY, label: 'Hồ sơ tạm' },
  { value: PatientStatus.ACTIVE, label: 'Đã xác minh' },
  { value: PatientStatus.MERGED, label: 'Đã gộp' }
];

export const ID_TYPE_OPTIONS = [
  { value: IdType.CCCD, label: 'CCCD' },
  { value: IdType.PASSPORT, label: 'Hộ chiếu' },
  { value: IdType.BHYT, label: 'Thẻ BHYT' }
];
