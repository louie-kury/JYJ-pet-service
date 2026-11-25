package com.louiekury.pet.vaccine.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.vaccine.application.createvaccine.CreateVaccineUseCase;
import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import com.louiekury.pet.vaccine.application.listvaccines.ListVaccinesUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VaccineWebAdapter {
    private final CreateVaccineUseCase useCase;
    private final ListVaccinesUseCase listVaccinesUseCase;

    public ResponseEntity<ApiResponse<List<VaccineResult>>> listVaccines() {
        return ResponseEntity.ok(ApiResponse.success(listVaccinesUseCase.listVaccines()));
    }

    public ResponseEntity<ApiResponse<VaccineResult>> createVaccine(CreateVaccineRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(useCase.createVaccine(request.getName())));
    }
}
