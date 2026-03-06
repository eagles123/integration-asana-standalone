package com.baker.integration.asana.model.connection;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TokenTypeConverter implements AttributeConverter<TokenType, String> {

    @Override
    public String convertToDatabaseColumn(TokenType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public TokenType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : TokenType.fromValue(dbData);
    }
}
