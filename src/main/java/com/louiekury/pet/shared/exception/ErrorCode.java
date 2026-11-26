package com.louiekury.pet.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VACCINE_NAME_NOT_FOUND("접종명을 찾을 수 없습니다."),
    VACCINE_NAME_DUPLICATE("이미 등록된 접종명입니다."),
    VACCINE_PERSISTENCE_UNAVAILABLE("접종명을 저장할 수 없습니다."),
    VACCINE_QUERY_UNAVAILABLE("접종명 목록을 조회할 수 없습니다."),
    EMPTY_UPDATE("수정할 항목이 없습니다."),
    VALIDATION_ERROR("요청 값이 올바르지 않습니다."),
    MEDIA_SERVICE_UNAVAILABLE("프로필 미디어 연결을 아직 사용할 수 없습니다."),
    REFERENCE_DATA_UNAVAILABLE("종·품종 기준 데이터를 조회할 수 없습니다."),
    PERSISTENCE_UNAVAILABLE("반려동물을 저장할 수 없습니다."),
    PET_QUERY_UNAVAILABLE("반려동물 목록을 조회할 수 없습니다."),
    IDEMPOTENCY_CONFLICT("이미 사용된 등록 요청 키입니다."),
    AUTH_SERVICE_UNAVAILABLE("인증 서비스를 사용할 수 없습니다."),
    UNAUTHORIZED("인증이 필요합니다."),
    FORBIDDEN("접근 권한이 없습니다."),
    NOT_FOUND("대상을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("지원하지 않는 요청 방식입니다."),
    INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.");

    private final String message;
}
