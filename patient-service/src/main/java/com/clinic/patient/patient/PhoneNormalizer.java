package com.clinic.patient.patient;

public final class PhoneNormalizer {

    private PhoneNormalizer() {
    }

    /**
     * Chuẩn hóa số điện thoại theo CS20:
     * Bỏ dấu cách, dấu chấm, dấu gạch; +84... hoặc 84... đổi thành 0...
     */
    public static String normalize(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String cleaned = phone.replaceAll("[\\s.\\-]+", "").trim();
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        } else if (cleaned.startsWith("84") && cleaned.length() == 11) {
            cleaned = "0" + cleaned.substring(2);
        }
        return cleaned;
    }

    public static boolean isValid(String phone) {
        return phone != null && phone.matches("^0\\d{9}$");
    }
}
