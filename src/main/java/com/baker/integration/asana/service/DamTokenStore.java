package com.baker.integration.asana.service;

import com.baker.integration.asana.model.auth.DamTokenInfo;
import com.baker.integration.asana.model.auth.LoginState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DamTokenStore {

    private static final long LOGIN_STATE_TTL_SECONDS = 600; // 10 minutes

    private final ConcurrentHashMap<String, DamTokenInfo> tokensByAsanaUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LoginState> pendingStates = new ConcurrentHashMap<>();

    public boolean hasValidToken(String asanaUserGid) {
        DamTokenInfo info = tokensByAsanaUser.get(asanaUserGid);
        return info != null && !info.isExpired();
    }

    public Optional<DamTokenInfo> getToken(String asanaUserGid) {
        DamTokenInfo info = tokensByAsanaUser.get(asanaUserGid);
        if (info == null || info.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(info);
    }

    public void storeToken(String asanaUserGid, DamTokenInfo tokenInfo) {
        tokensByAsanaUser.put(asanaUserGid, tokenInfo);
    }

    public void removeToken(String asanaUserGid) {
        tokensByAsanaUser.remove(asanaUserGid);
    }

    public String createLoginState(String asanaUserGid, String asanaWorkspaceGid, String realm) {
        String state = UUID.randomUUID().toString();
        pendingStates.put(state, new LoginState(asanaUserGid, asanaWorkspaceGid, realm));
        return state;
    }

    public Optional<LoginState> peekLoginState(String stateToken) {
        LoginState state = pendingStates.get(stateToken);
        if (state == null) {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() - state.getCreatedAtEpochSeconds() > LOGIN_STATE_TTL_SECONDS) {
            pendingStates.remove(stateToken);
            return Optional.empty();
        }
        return Optional.of(state);
    }

    public Optional<LoginState> consumeLoginState(String stateToken) {
        LoginState state = pendingStates.remove(stateToken);
        if (state == null) {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() - state.getCreatedAtEpochSeconds() > LOGIN_STATE_TTL_SECONDS) {
            return Optional.empty();
        }
        return Optional.of(state);
    }
}
