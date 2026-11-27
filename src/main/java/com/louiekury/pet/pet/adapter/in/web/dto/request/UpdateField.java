package com.louiekury.pet.pet.adapter.in.web.dto.request;

import lombok.Getter;

@Getter
public final class UpdateField<T> {
    private final boolean present;
    private final T value;

    private UpdateField(boolean present, T value) {
        this.present = present;
        this.value = value;
    }

    public static <T> UpdateField<T> absent() {
        return new UpdateField<>(false, null);
    }

    public static <T> UpdateField<T> of(T value) {
        return new UpdateField<>(true, value);
    }
}
