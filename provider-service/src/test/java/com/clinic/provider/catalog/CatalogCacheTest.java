package com.clinic.provider.catalog;

import com.clinic.provider.catalog.dto.SpecialtyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(CatalogCacheTest.TestConfig.class)
class CatalogCacheTest {

    @Configuration
    @EnableCaching
    static class TestConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("specialties", "doctors");
        }

        @Bean
        SpecialtyRepository specialtyRepository() {
            return mock(SpecialtyRepository.class);
        }

        @Bean
        RoomRepository roomRepository() {
            return mock(RoomRepository.class);
        }

        @Bean
        DoctorRepository doctorRepository() {
            return mock(DoctorRepository.class);
        }

        @Bean
        CatalogService catalogService(SpecialtyRepository specialtyRepository, RoomRepository roomRepository,
                                      DoctorRepository doctorRepository) {
            return new CatalogService(specialtyRepository, roomRepository, doctorRepository);
        }
    }

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Test
    void listSpecialties_cachesResultAndEvictsOnCreate() {
        Specialty s = new Specialty();
        s.setId(1L);
        s.setCode("NOI");
        s.setName("Nội tổng quát");
        s.setActive(true);
        when(specialtyRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(s));

        assertEquals(1, catalogService.listSpecialties().size());
        assertEquals(1, catalogService.listSpecialties().size());
        verify(specialtyRepository, times(1)).findByActiveTrueOrderByNameAsc();

        when(specialtyRepository.existsByCode("TIM")).thenReturn(false);
        when(specialtyRepository.existsByName("Tim mạch")).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(inv -> {
            Specialty saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        catalogService.createSpecialty(new SpecialtyRequest("TIM", "Tim mạch", null, true));

        catalogService.listSpecialties();
        verify(specialtyRepository, times(2)).findByActiveTrueOrderByNameAsc();
    }
}

