package com.louiekury.pet.global.persistence;

import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
public abstract class BaseDocument {

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private Instant deletedAt;

    /** 원자 insert로 결과 스냅샷까지 보존하는 어댑터가 동일한 생성 시각을 설정한다. */
    protected void initializeTimestamps(Instant createdAt) {
        if (this.createdAt != null || this.updatedAt != null) {
            throw new IllegalStateException("감사 시각이 이미 초기화됐습니다.");
        }
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = createdAt;
    }

    public void markDeleted(Instant deletedAt) {
        if (this.deletedAt == null) {
            this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt");
        }
    }
}
