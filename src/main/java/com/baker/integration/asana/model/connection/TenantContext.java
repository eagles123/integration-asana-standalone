package com.baker.integration.asana.model.connection;

public class TenantContext {

    private final String workspaceGid;
    private final String tenantId;
    private final String keycloakRealm;

    public TenantContext(String workspaceGid, String tenantId, String keycloakRealm) {
        this.workspaceGid = workspaceGid;
        this.tenantId = tenantId;
        this.keycloakRealm = keycloakRealm;
    }

    public String getWorkspaceGid() {
        return workspaceGid;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getKeycloakRealm() {
        return keycloakRealm;
    }
}
