package com.clinic.patient.patient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameNormalizerTest {

    @Test
    void boDauVietThuongGopKhoangTrang() {
        assertThat(NameNormalizer.normalize("  Lê   Thị Hoa ")).isEqualTo("le thi hoa");
    }

    @Test
    void chuDDuocThayRieng() {
        assertThat(NameNormalizer.normalize("Đặng Đức Độ")).isEqualTo("dang duc do");
    }

    @Test
    void tenKhongDauGiuNguyen() {
        assertThat(NameNormalizer.normalize("Le Thi Hoa")).isEqualTo(NameNormalizer.normalize("Lê Thị Hoa"));
    }

    @Test
    void nullThanhChuoiRong() {
        assertThat(NameNormalizer.normalize(null)).isEmpty();
    }
}
