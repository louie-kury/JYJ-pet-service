package com.louiekury.pet.vaccine.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.vaccine.adapter.in.web.dto.request.CreateVaccineRequestDto;
import com.louiekury.pet.vaccine.adapter.in.web.dto.response.VaccineResponseDto;
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

    public ResponseEntity<ApiResponse<List<VaccineResponseDto>>> getVaccines() {
        return ResponseEntity.ok(ApiResponse.success(getVaccinesUseCase.getVaccines().stream()
            .map(VaccineResponseDto::from)
            .toList()));
    }

    public ResponseEntity<ApiResponse<VaccineResponseDto>> createVaccine(CreateVaccineRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(VaccineResponseDto.from(useCase.createVaccine(request.getName()))));
    }
}
