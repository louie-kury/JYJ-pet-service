package com.louiekury.pet.vaccine.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import com.louiekury.pet.vaccine.application.port.in.CreateVaccineUseCase;
import com.louiekury.pet.vaccine.application.port.in.GetVaccinesUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VaccineWebAdapter {
    private final CreateVaccineUseCase useCase;
    private final GetVaccinesUseCase getVaccinesUseCase;

    public ResponseEntity<ApiResponse<List<VaccineResult>>> getVaccines() {
        return ResponseEntity.ok(ApiResponse.success(getVaccinesUseCase.getVaccines()));
    }

    public ResponseEntity<ApiResponse<VaccineResult>> createVaccine(CreateVaccineRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(useCase.createVaccine(request.getName())));
    }
}
