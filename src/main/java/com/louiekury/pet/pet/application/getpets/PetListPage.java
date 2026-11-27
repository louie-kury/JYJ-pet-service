package com.louiekury.pet.pet.application.getpets;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PetListPage {

    private final List<PetListData> content;
    private final long totalElements;

    public static PetListPage of(List<PetListData> content, long totalElements) {
        return PetListPage.builder()
            .content(List.copyOf(content))
            .totalElements(totalElements)
            .build();
    }
}
