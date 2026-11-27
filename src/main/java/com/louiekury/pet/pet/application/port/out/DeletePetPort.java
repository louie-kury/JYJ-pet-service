package com.louiekury.pet.pet.application.port.out;

import java.time.Instant;

public interface DeletePetPort {
    boolean delete(String id, String caregiverId, Instant deletedAt);
}
