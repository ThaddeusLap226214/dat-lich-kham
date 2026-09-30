package com.clinic.provider.catalog;

import com.clinic.provider.catalog.dto.SpecialtyRequest;
import com.clinic.provider.catalog.dto.SpecialtyResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
public class SpecialtyController {

    private final CatalogService catalogService;

    public SpecialtyController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    public List<SpecialtyResponse> list() {
        return catalogService.listSpecialties();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyResponse create(@Valid @RequestBody SpecialtyRequest request) {
        return catalogService.createSpecialty(request);
    }

    @PutMapping("/{id}")
    public SpecialtyResponse update(@PathVariable Long id, @Valid @RequestBody SpecialtyRequest request) {
        return catalogService.updateSpecialty(id, request);
    }
}
