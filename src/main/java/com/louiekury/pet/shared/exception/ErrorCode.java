package com.louiekury.pet.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_ERROR("요청 값이 올바르지 않습니다."),
    MEDIA_SERVICE_UNAVAILABLE("프로필 미디어 연결을 아직 사용할 수 없습니다."),
    REFERENCE_DATA_UNAVAILABLE("종·품종 기준 데이터를 조회할 수 없습니다."),
    PERSISTENCE_UNAVAILABLE("반려동물을 저장할 수 없습니다."),
    IDEMPOTENCY_CONFLICT("이미 사용된 등록 요청 키입니다."),
    UNAUTHORIZED("인증이 필요합니다."),
    FORBIDDEN("접근 권한이 없습니다."),
    NOT_FOUND("대상을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("지원하지 않는 요청 방식입니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String message;
}
