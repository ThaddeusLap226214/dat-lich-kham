export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER'
}

export interface GenderOption {
  value: Gender;
  label: string;
}

export const GENDER_OPTIONS: GenderOption[] = [
  { value: Gender.MALE, label: 'Nam' },
  { value: Gender.FEMALE, label: 'Nữ' },
  { value: Gender.OTHER, label: 'Khác' }
];

export const GENDER_ALL_OPTION = { value: 'ALL', label: 'Tất cả' };