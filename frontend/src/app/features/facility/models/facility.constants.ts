import { FacilityType } from './facility.model';

export const FACILITY_TYPE_OPTIONS = [
  { value: FacilityType.HOSPITAL, label: 'Bệnh viện' },
  { value: FacilityType.CLINIC, label: 'Phòng khám' },
  { value: FacilityType.OTHER, label: 'Khác' }
];

export const FACILITY_STATUS_OPTIONS = [
  { value: true, label: 'Đang hoạt động' },
  { value: false, label: 'Ngừng hoạt động' }
];
