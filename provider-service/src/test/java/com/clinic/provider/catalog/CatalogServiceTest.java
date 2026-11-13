package com.clinic.provider.catalog;

import com.clinic.provider.catalog.dto.DoctorRequest;
import com.clinic.provider.catalog.dto.DoctorResponse;
import com.clinic.provider.catalog.dto.RoomRequest;
import com.clinic.provider.catalog.dto.RoomResponse;
import com.clinic.provider.catalog.dto.SpecialtyRequest;
import com.clinic.provider.catalog.dto.SpecialtyResponse;
import com.clinic.provider.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private DoctorRepository doctorRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(specialtyRepository, roomRepository, doctorRepository);
    }

    @Test
    void listSpecialties_returnsActiveSpecialties() {
        Specialty s = specialty(1L, "NOI", "Nội tổng quát", true);
        when(specialtyRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(s));

        List<SpecialtyResponse> result = catalogService.listSpecialties();
        assertEquals(1, result.size());
        assertEquals("NOI", result.getFirst().code());
    }

    @Test
    void createSpecialty_createsWhenUnique() {
        when(specialtyRepository.existsByCode("DA_LIEU")).thenReturn(false);
        when(specialtyRepository.existsByName("Da liễu")).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(inv -> {
            Specialty s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SpecialtyResponse res = catalogService.createSpecialty(
            new SpecialtyRequest(" DA_LIEU ", " Da liễu ", " Khám da ", null));

        assertEquals(10L, res.id());
        assertEquals("DA_LIEU", res.code());
        assertEquals("Da liễu", res.name());
        assertTrue(res.active());
    }

    @Test
    void createSpecialty_throwsConflictOnDuplicateCodeOrName() {
        when(specialtyRepository.existsByCode("NOI")).thenReturn(true);
        BusinessException codeEx = assertThrows(BusinessException.class, () ->
            catalogService.createSpecialty(new SpecialtyRequest("NOI", "Nội", null, true)));
        assertEquals(HttpStatus.CONFLICT, codeEx.getStatus());
        assertEquals("CODE_TAKEN", codeEx.getCode());

        when(specialtyRepository.existsByCode("TIM")).thenReturn(false);
        when(specialtyRepository.existsByName("Nội tổng quát")).thenReturn(true);
        BusinessException nameEx = assertThrows(BusinessException.class, () ->
            catalogService.createSpecialty(new SpecialtyRequest("TIM", "Nội tổng quát", null, true)));
        assertEquals(HttpStatus.CONFLICT, nameEx.getStatus());
        assertEquals("NAME_TAKEN", nameEx.getCode());
    }

    @Test
    void updateSpecialty_updatesOrThrows() {
        Specialty existing = specialty(1L, "NOI", "Nội tổng quát", true);
        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(specialtyRepository.existsByCodeAndIdNot("NOI_2", 1L)).thenReturn(false);
        when(specialtyRepository.existsByNameAndIdNot("Nội 2", 1L)).thenReturn(false);
        when(specialtyRepository.save(any(Specialty.class))).thenAnswer(inv -> inv.getArgument(0));

        SpecialtyResponse updated = catalogService.updateSpecialty(1L,
            new SpecialtyRequest("NOI_2", "Nội 2", "Mô tả", false));
        assertEquals("NOI_2", updated.code());
        assertFalse(updated.active());

        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());
        BusinessException notFound = assertThrows(BusinessException.class, () ->
            catalogService.updateSpecialty(99L, new SpecialtyRequest("X", "Y", null, true)));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());
    }

    @Test
    void rooms_crudAndFindByIds() {
        Room r = room(1L, "P101", "Phòng 101");
        when(roomRepository.findAll(Sort.by("code"))).thenReturn(List.of(r));
        assertEquals(1, catalogService.listRooms().size());

        when(roomRepository.findByIdIn(List.of(1L))).thenReturn(List.of(r));
        assertEquals("P101", catalogService.findRoomsByIds(List.of(1L)).getFirst().code());

        when(roomRepository.existsByCode("P101")).thenReturn(true);
        BusinessException ex = assertThrows(BusinessException.class, () ->
            catalogService.createRoom(new RoomRequest("P101", "Phòng mới", "Tầng 1", true)));
        assertEquals("CODE_TAKEN", ex.getCode());

        when(roomRepository.findById(1L)).thenReturn(Optional.of(r));
        when(roomRepository.existsByCodeAndIdNot("P102", 1L)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));
        RoomResponse updated = catalogService.updateRoom(1L, new RoomRequest("P102", "Phòng 102", "Tầng 1", true));
        assertEquals("P102", updated.code());
    }

    @Test
    void doctors_listGetCreateUpdateAndFindByIds() {
        Specialty noi = specialty(1L, "NOI", "Nội tổng quát", true);
        Doctor d1 = doctor(1L, "Nguyễn Văn An", noi);
        Doctor d2 = doctor(2L, "Trần Thị Bình", noi);

        when(doctorRepository.findByActiveTrue()).thenReturn(List.of(d1, d2));
        assertEquals(2, catalogService.listDoctors(null).size());

        when(doctorRepository.findBySpecialtyIdAndActiveTrue(1L)).thenReturn(List.of(d1, d2));
        assertEquals(2, catalogService.listDoctors(1L).size());

        when(doctorRepository.findById(1L)).thenReturn(Optional.of(d1));
        assertEquals("Nguyễn Văn An", catalogService.getDoctor(1L).fullName());

        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());
        BusinessException notFound = assertThrows(BusinessException.class, () -> catalogService.getDoctor(99L));
        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatus());

        when(doctorRepository.findByIdIn(List.of(1L, 2L))).thenReturn(List.of(d1, d2));
        assertEquals(2, catalogService.findDoctorsByIds(List.of(1L, 2L)).size());

        when(specialtyRepository.findById(1L)).thenReturn(Optional.of(noi));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> {
            Doctor saved = inv.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(7L);
            }
            return saved;
        });
        DoctorResponse created = catalogService.createDoctor(
            new DoctorRequest("Lê Văn Cường", "BS", 1L, "0901234567", "cuong@clinic.local", null));
        assertEquals(7L, created.id());
        assertEquals(1L, created.specialtyId());
        assertTrue(created.active());

        when(specialtyRepository.findById(99L)).thenReturn(Optional.empty());
        BusinessException badSpec = assertThrows(BusinessException.class, () ->
            catalogService.updateDoctor(1L, new DoctorRequest("A", null, 99L, null, null, true)));
        assertEquals(HttpStatus.NOT_FOUND, badSpec.getStatus());
    }

    private Specialty specialty(Long id, String code, String name, boolean active) {
        Specialty s = new Specialty();
        s.setId(id);
        s.setCode(code);
        s.setName(name);
        s.setActive(active);
        return s;
    }

    private Room room(Long id, String code, String name) {
        Room r = new Room();
        r.setId(id);
        r.setCode(code);
        r.setName(name);
        r.setActive(true);
        return r;
    }

    private Doctor doctor(Long id, String fullName, Specialty specialty) {
        Doctor d = new Doctor();
        d.setId(id);
        d.setFullName(fullName);
        d.setTitle("BS.CKI");
        d.setSpecialty(specialty);
        d.setActive(true);
        return d;
    }
}

