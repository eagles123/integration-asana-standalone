package com.baker.integration.asana.model.auth;

import java.time.Instant;

public class DamTokenInfo {

    private final String accessToken;
    private final String refreshToken;
    private final String realm;
    private final long expiresAtEpochSeconds;

    public DamTokenInfo(String accessToken, String refreshToken, long expiresInSeconds, String realm) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.realm = realm;
        this.expiresAtEpochSeconds = Instant.now().getEpochSecond() + expiresInSeconds;
    }

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getRealm() { return realm; }
    public long getExpiresAtEpochSeconds() { return expiresAtEpochSeconds; }

    public boolean isExpired() {
        return Instant.now().getEpochSecond() >= (expiresAtEpochSeconds - 30);
    }
}
