package com.louiekury.pet.global.web.exception;

import com.louiekury.pet.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public final class ErrorCodeHttpMapper {

    private ErrorCodeHttpMapper() {
    }

    public static HttpStatus toStatus(ErrorCode code) {
        return switch (code) {
            case VALIDATION_ERROR, EMPTY_UPDATE -> HttpStatus.BAD_REQUEST;
            case IDEMPOTENCY_CONFLICT, VACCINE_NAME_DUPLICATE -> HttpStatus.CONFLICT;
            case MEDIA_SERVICE_UNAVAILABLE, REFERENCE_DATA_UNAVAILABLE,
                PERSISTENCE_UNAVAILABLE, PET_QUERY_UNAVAILABLE, VACCINE_PERSISTENCE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case INTERNAL_SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
