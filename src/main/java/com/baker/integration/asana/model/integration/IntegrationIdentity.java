package com.baker.integration.asana.model.integration;

public class IntegrationIdentity {

    private final String tenantId;
    private final String damUserId;
    private final String asanaUserId;
    private final String asanaWorkspaceId;

    public IntegrationIdentity(String tenantId, String damUserId, String asanaUserId, String asanaWorkspaceId) {
        this.tenantId = tenantId;
        this.damUserId = damUserId;
        this.asanaUserId = asanaUserId;
        this.asanaWorkspaceId = asanaWorkspaceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDamUserId() {
        return damUserId;
    }

    public String getAsanaUserId() {
        return asanaUserId;
    }

    public String getAsanaWorkspaceId() {
        return asanaWorkspaceId;
    }
}
