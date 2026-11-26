package com.louiekury.pet.pet.adapter.in.web;

import com.louiekury.pet.pet.application.deletepet.DeletePetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets")
public class PetDeleteController {
    private final DeletePetService service;

    @DeleteMapping("/{petId}")
    public ResponseEntity<Void> delete(@PathVariable String petId,
            @RequestAttribute(PetRegistrationAuthInterceptor.CAREGIVER_ID) String caregiverId) {
        service.delete(petId, caregiverId);
        return ResponseEntity.noContent().build();
    }
}
