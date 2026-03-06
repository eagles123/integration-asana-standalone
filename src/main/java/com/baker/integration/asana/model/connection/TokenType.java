package com.baker.integration.asana.model.connection;

public enum TokenType {
    REFRESH("refresh"),
    OFFLINE("offline");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TokenType fromValue(String rawValue) {
        for (TokenType tokenType : values()) {
            if (tokenType.value.equalsIgnoreCase(rawValue)) {
                return tokenType;
            }
        }
        throw new IllegalArgumentException("Unknown token type: " + rawValue);
    }
}
