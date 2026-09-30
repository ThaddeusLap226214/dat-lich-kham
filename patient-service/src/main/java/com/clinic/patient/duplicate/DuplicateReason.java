package com.clinic.patient.duplicate;

// Ứng với tình huống 2, 3, 5 trong bảng tình huống của Patient Service (docs/02 mục 4.1)
public enum DuplicateReason {
    SAME_ID_DIFFERENT_INFO,
    SAME_NAME_DOB_PHONE,
    ID_ADDED_BELONGS_TO_OTHER
}
