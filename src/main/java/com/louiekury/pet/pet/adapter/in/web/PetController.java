package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.global.security.AuthenticatedMember;
import org.springframework.web.bind.annotation.PathVariable;
import com.louiekury.pet.pet.adapter.in.web.dto.request.CreatePetRequestDto;
import com.louiekury.pet.pet.adapter.in.web.dto.response.PetDetailResponseDto;
import com.louiekury.pet.pet.adapter.in.web.dto.response.PetListResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets")
public class PetController {

    private final PetWebAdapter petWebAdapter;
    private final AuthenticatedMember authenticatedMember;

    @GetMapping("/{petId}")
    public ResponseEntity<ApiResponse<PetDetailResponseDto>> getPet(
            @PathVariable String petId, @RequestHeader(value = "Authorization", required = false) String authorization) {
        return petWebAdapter.getPet(petId, authenticatedMember.require(authorization));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PetListResponseDto>> getPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size) {
        return petWebAdapter.getPets(page, size);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PetDetailResponseDto>> createPet(
            @Valid @RequestBody CreatePetRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String caregiverId = authorization == null ? null : authenticatedMember.require(authorization);
        return petWebAdapter.createPet(request, idempotencyKey, caregiverId);
    }
}
