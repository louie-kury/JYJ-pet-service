package com.louiekury.pet.pet.adapter.in.web.dto.response;

import com.louiekury.pet.pet.application.getpets.GetPetsResult;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class PetListResponseDto {

    private final List<PetListItemResponseDto> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final long totalPages;
    private final boolean hasNext;

    public static PetListResponseDto from(GetPetsResult result) {
        return PetListResponseDto.builder()
            .content(result.getContent().stream()
                .map(PetListItemResponseDto::from).toList())
            .page(result.getPage())
            .size(result.getSize())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .hasNext(result.isHasNext())
            .build();
    }
}
