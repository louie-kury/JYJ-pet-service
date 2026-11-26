package com.louiekury.pet.pet.application.deletepet;

import java.time.Instant;

public interface DeletePetPort {
    boolean delete(String id, String caregiverId, Instant deletedAt);
}
