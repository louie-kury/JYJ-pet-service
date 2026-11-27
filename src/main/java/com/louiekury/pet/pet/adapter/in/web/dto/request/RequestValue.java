package com.louiekury.pet.pet.adapter.in.web.dto.request;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

final class RequestValue {
    private RequestValue() { }

    static String text(Object value) {
        if (value == null || value instanceof String) {
            return (String) value;
        }
        throw invalid();
    }

    static Boolean bool(Object value) {
        if (value == null || value instanceof Boolean) {
            return (Boolean) value;
        }
        throw invalid();
    }

    static BigDecimal decimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Integer || value instanceof Long || value instanceof java.math.BigInteger) {
            return new BigDecimal(value.toString());
        }
        throw invalid();
    }

    static LocalDate date(Object value) {
        String text = text(value);
        if (text == null) {
            return null;
        }
        if (!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw invalid();
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException exception) {
            throw invalid();
        }
    }

    static BusinessException invalid() {
        return new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
}
