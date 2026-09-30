package com.clinic.patient.duplicate;

import com.clinic.patient.patient.NameNormalizer;
import com.clinic.patient.patient.Patient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Chuyển từ PatientMatchingServiceImpl.calculateMatchScore (repo MPI của Như).
// Bỏ getAutoDecision: điểm chỉ ghi vào duplicate_candidates.score để lễ tân tham khảo, không dùng để tự gộp.
@Component
public class MatchScorer {

    private static final BigDecimal IDENTITY = new BigDecimal("40");
    private static final BigDecimal NAME = new BigDecimal("25");
    private static final BigDecimal DATE_OF_BIRTH = new BigDecimal("20");
    private static final BigDecimal PHONE = new BigDecimal("10");
    private static final BigDecimal GENDER = new BigDecimal("5");

    public BigDecimal score(Patient a, Patient b) {
        BigDecimal score = BigDecimal.ZERO;

        if (a.getIdType() != null && a.getIdType() == b.getIdType() && sameText(a.getIdNumber(), b.getIdNumber())) {
            score = score.add(IDENTITY);
        }
        score = score.add(NAME.multiply(BigDecimal.valueOf(nameSimilarity(a.getFullName(), b.getFullName()))));
        if (a.getDateOfBirth() != null && a.getDateOfBirth().equals(b.getDateOfBirth())) {
            score = score.add(DATE_OF_BIRTH);
        }
        if (sameText(a.getPhone(), b.getPhone())) {
            score = score.add(PHONE);
        }
        if (a.getGender() != null && a.getGender() == b.getGender()) {
            score = score.add(GENDER);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    // Trùng hẳn sau chuẩn hóa = 1; tên này nằm trọn trong tên kia (thiếu tên đệm) = 0,85; còn lại = 0
    double nameSimilarity(String x, String y) {
        if (x == null || y == null) {
            return 0.0;
        }
        String a = NameNormalizer.normalize(x);
        String b = NameNormalizer.normalize(y);
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }
        return a.contains(b) || b.contains(a) ? 0.85 : 0.0;
    }

    private static boolean sameText(String x, String y) {
        return x != null && y != null && x.trim().equalsIgnoreCase(y.trim());
    }
}
