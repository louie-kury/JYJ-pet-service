package com.louiekury.pet.registration.adapter.in.web;

import com.louiekury.pet.global.web.response.ApiResponse;
import com.louiekury.pet.registration.adapter.in.web.dto.request.RegisterPetRequestDto;
import com.louiekury.pet.registration.adapter.in.web.dto.response.PetDetailResponseDto;
import com.louiekury.pet.registration.application.model.PetResult;
import com.louiekury.pet.registration.application.model.RegisterPetCommand;
import com.louiekury.pet.registration.application.port.in.RegisterPetUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PetRegistrationController {

    private final RegisterPetUseCase useCase;

    @PostMapping(value = "/api/pets", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<PetDetailResponseDto>> register(
            @Valid @RequestBody RegisterPetRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        PetResult result = useCase.register(RegisterPetCommand.of(request.getName(),
            request.getSpeciesId(), request.getBreedId(), request.getProfileMediaId(),
            request.getBirth(), request.getSex(), request.getIsNeutered(), request.getWeight(),
            request.getBasicVaccinationCompleted(), idempotencyKey));
        return ResponseEntity.created(URI.create("/api/pets/" + result.getPetId()))
            .body(ApiResponse.success(PetDetailResponseDto.from(result)));
    }
}
