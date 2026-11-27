package com.louiekury.pet.pet.application.getpets;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class GetPetsResult {

    private final List<PetListItemResult> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final long totalPages;
    private final boolean hasNext;

    public static GetPetsResult of(List<PetListItemResult> content, GetPetsQuery query, long totalElements) {
        long totalPages = totalElements / query.getSize()
            + (totalElements % query.getSize() == 0 ? 0 : 1);
        return GetPetsResult.builder()
            .content(List.copyOf(content))
            .page(query.getPage())
            .size(query.getSize())
            .totalElements(totalElements)
            .totalPages(totalPages)
            .hasNext((long) query.getPage() + 1 < totalPages)
            .build();
    }
}
