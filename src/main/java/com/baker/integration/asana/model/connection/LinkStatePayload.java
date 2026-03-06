package com.baker.integration.asana.model.connection;

public class LinkStatePayload {

    private String nonce;
    private String asanaWorkspaceGid;
    private String asanaUserGid;
    private String tenantId;
    private long expEpochSeconds;

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getAsanaWorkspaceGid() {
        return asanaWorkspaceGid;
    }

    public void setAsanaWorkspaceGid(String asanaWorkspaceGid) {
        this.asanaWorkspaceGid = asanaWorkspaceGid;
    }

    public String getAsanaUserGid() {
        return asanaUserGid;
    }

    public void setAsanaUserGid(String asanaUserGid) {
        this.asanaUserGid = asanaUserGid;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public long getExpEpochSeconds() {
        return expEpochSeconds;
    }

    public void setExpEpochSeconds(long expEpochSeconds) {
        this.expEpochSeconds = expEpochSeconds;
    }
}
