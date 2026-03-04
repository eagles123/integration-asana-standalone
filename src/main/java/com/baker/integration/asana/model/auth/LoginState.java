package com.baker.integration.asana.model.auth;

import java.time.Instant;

public class LoginState {

    private final String asanaUserGid;
    private final String asanaWorkspaceGid;
    private final String realm;
    private final long createdAtEpochSeconds;

    public LoginState(String asanaUserGid, String asanaWorkspaceGid, String realm) {
        this.asanaUserGid = asanaUserGid;
        this.asanaWorkspaceGid = asanaWorkspaceGid;
        this.realm = realm;
        this.createdAtEpochSeconds = Instant.now().getEpochSecond();
    }

    public String getAsanaUserGid() { return asanaUserGid; }
    public String getAsanaWorkspaceGid() { return asanaWorkspaceGid; }
    public String getRealm() { return realm; }
    public long getCreatedAtEpochSeconds() { return createdAtEpochSeconds; }
}
