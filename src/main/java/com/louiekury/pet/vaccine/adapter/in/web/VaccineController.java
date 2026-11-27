package com.louiekury.pet.vaccine.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.vaccine.application.createvaccine.VaccineResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vaccine-names")
@RequiredArgsConstructor
public class VaccineController {
    private final VaccineWebAdapter adapter;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VaccineResult>>> getVaccines() {
        return adapter.getVaccines();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<VaccineResult>> createVaccine(@RequestBody CreateVaccineRequestDto request) {
        return adapter.createVaccine(request);
    }
}
