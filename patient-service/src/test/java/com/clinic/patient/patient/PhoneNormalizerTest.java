package com.clinic.patient.patient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNormalizerTest {

    @Test
    void chuanHoaSoDienThoaiTheoCS20() {
        assertThat(PhoneNormalizer.normalize("+84 935 111 222")).isEqualTo("0935111222");
        assertThat(PhoneNormalizer.normalize("84935111222")).isEqualTo("0935111222");
        assertThat(PhoneNormalizer.normalize("0935.111.222")).isEqualTo("0935111222");
        assertThat(PhoneNormalizer.normalize("0935-111-222")).isEqualTo("0935111222");
        assertThat(PhoneNormalizer.normalize("0905123456")).isEqualTo("0905123456");
        assertThat(PhoneNormalizer.normalize(null)).isNull();
        assertThat(PhoneNormalizer.normalize("   ")).isNull();
    }
}
