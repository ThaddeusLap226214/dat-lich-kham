import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function phoneValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; 
    }

    const phonePattern = /^0\d{9}$/;
    const valid = phonePattern.test(control.value);

    return valid ? null : { invalidPhone: { value: control.value } };
  };
}

export function formatPhoneNumber(phone: string | null | undefined): string {
  if (!phone) return '-';
  return phone.replace(/(\d{4})(\d{3})(\d{3})/, '$1 $2 $3');
}


export const PHONE_ERROR_MESSAGE = 'Số điện thoại không hợp lệ';


export const PHONE_PATTERN = /^0\d{9}$/;
