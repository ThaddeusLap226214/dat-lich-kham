package com.clinic.patient.patient;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

// Chuyển từ PatientMatchingServiceImpl.normalizeVietnamese (repo MPI của Như)
public final class NameNormalizer {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private NameNormalizer() {
    }

    // "  Lê  Thị Hoa " → "le thi hoa"; chữ Đ không tách được dấu bằng NFD nên thay riêng
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String noMarks = DIACRITICS.matcher(Normalizer.normalize(name, Normalizer.Form.NFD)).replaceAll("");
        String lower = noMarks.replace('Đ', 'd').replace('đ', 'd').toLowerCase(Locale.ROOT).trim();
        return SPACES.matcher(lower).replaceAll(" ");
    }
}
