package com.clinic.patient.duplicate;

import com.clinic.patient.patient.Gender;
import com.clinic.patient.patient.IdType;
import com.clinic.patient.patient.Patient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class MatchScorerTest {

    private final MatchScorer scorer = new MatchScorer();

    @Test
    void trungDuMoiTruongDuoc100Diem() {
        Patient a = patient("Lê Thị Hoa", IdType.CCCD, "048190001234", "0905123456");
        Patient b = patient("Le Thi Hoa", IdType.CCCD, "048190001234", "0905123456");
        assertThat(scorer.score(a, b)).isEqualByComparingTo("100");
    }

    @Test
    void hoSoMauSo3SoVoiHoSo1KhongCoGiayTo() {
        Patient a = patient("Lê Thị Hoa", IdType.CCCD, "048190001234", "0905123456");
        Patient b = patient("Le Thi Hoa", null, null, "0905123456");
        // tên 25 + ngày sinh 20 + SĐT 10 + giới tính 5
        assertThat(scorer.score(a, b)).isEqualByComparingTo("60");
    }

    @Test
    void thieuTenDemDuoc85PhanTramDiemTen() {
        assertThat(scorer.nameSimilarity("Nguyễn Văn An", "Nguyễn An")).isZero();
        assertThat(scorer.nameSimilarity("Nguyễn Văn An", "Văn An")).isEqualTo(0.85);
    }

    @Test
    void tenRongKhongDuocDiem() {
        assertThat(scorer.nameSimilarity("", "Lê Thị Hoa")).isZero();
    }

    private static Patient patient(String name, IdType idType, String idNumber, String phone) {
        Patient p = new Patient();
        p.setFullName(name);
        p.setDateOfBirth(LocalDate.of(1990, 5, 12));
        p.setGender(Gender.FEMALE);
        p.setIdType(idType);
        p.setIdNumber(idNumber);
        p.setPhone(phone);
        return p;
    }
}
