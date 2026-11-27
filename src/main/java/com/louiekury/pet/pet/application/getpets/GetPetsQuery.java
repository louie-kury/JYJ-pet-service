package com.louiekury.pet.pet.application.getpets;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class GetPetsQuery {

    private final int page;
    private final int size;
    private final String caregiverId;

    public static GetPetsQuery of(int page, int size, String caregiverId) {
        return GetPetsQuery.builder()
            .page(page)
            .size(size)
            .caregiverId(caregiverId)
            .build();
    }

    public long getOffset() {
        return (long) page * size;
    }
}
