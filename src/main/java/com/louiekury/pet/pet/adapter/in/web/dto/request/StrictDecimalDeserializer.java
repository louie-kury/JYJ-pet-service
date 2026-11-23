package com.louiekury.pet.pet.adapter.in.web.dto.request;

import com.louiekury.pet.shared.exception.BusinessException;
import com.louiekury.pet.shared.exception.ErrorCode;
import java.math.BigDecimal;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** 입력을 double로 반올림하지 않고 원래 십진 정밀도를 보존한다. */
public class StrictDecimalDeserializer extends ValueDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.currentToken().isNumeric()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        return parser.getDecimalValue();
    }
}
