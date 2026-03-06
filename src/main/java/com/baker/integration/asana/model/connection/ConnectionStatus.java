package com.baker.integration.asana.model.connection;

public enum ConnectionStatus {
    LINKED("linked"),
    NEEDS_REAUTH("needs_reauth"),
    REVOKED("revoked");

    private final String value;

    ConnectionStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ConnectionStatus fromValue(String rawValue) {
        for (ConnectionStatus status : values()) {
            if (status.value.equalsIgnoreCase(rawValue)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown connection status: " + rawValue);
    }
}
