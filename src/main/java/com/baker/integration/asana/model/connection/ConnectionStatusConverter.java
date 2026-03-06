package com.baker.integration.asana.model.connection;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ConnectionStatusConverter implements AttributeConverter<ConnectionStatus, String> {

    @Override
    public String convertToDatabaseColumn(ConnectionStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ConnectionStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ConnectionStatus.fromValue(dbData);
    }
}
