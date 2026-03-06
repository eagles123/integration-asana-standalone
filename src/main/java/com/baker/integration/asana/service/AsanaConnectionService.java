package com.baker.integration.asana.service;

import com.baker.integration.asana.model.connection.AsanaConnection;
import com.baker.integration.asana.model.connection.ConnectionStatus;
import com.baker.integration.asana.model.connection.TokenType;
import com.baker.integration.asana.model.keycloak.KeycloakTokenResponse;
import com.baker.integration.asana.repository.AsanaConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class AsanaConnectionService {

    private final AsanaConnectionRepository asanaConnectionRepository;
    private final TokenEncryptionService tokenEncryptionService;

    public AsanaConnectionService(AsanaConnectionRepository asanaConnectionRepository,
                                  TokenEncryptionService tokenEncryptionService) {
        this.asanaConnectionRepository = asanaConnectionRepository;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    public Optional<AsanaConnection> findByWorkspaceAndUser(String workspaceGid, String asanaUserGid) {
        return asanaConnectionRepository.findByAsanaWorkspaceGidAndAsanaUserGid(workspaceGid, asanaUserGid);
    }

    public Optional<AsanaConnection> findLinkedByWorkspaceAndUser(String workspaceGid, String asanaUserGid) {
        return findByWorkspaceAndUser(workspaceGid, asanaUserGid)
                .filter(c -> c.getStatus() == ConnectionStatus.LINKED);
    }

    @Transactional
    public AsanaConnection upsertLinkedConnection(String workspaceGid,
                                                  String asanaUserGid,
                                                  String tenantId,
                                                  String realm,
                                                  String keycloakUserId,
                                                  KeycloakTokenResponse tokenResponse) {
        AsanaConnection connection = asanaConnectionRepository
                .findByAsanaWorkspaceGidAndAsanaUserGid(workspaceGid, asanaUserGid)
                .orElseGet(AsanaConnection::new);

        connection.setAsanaWorkspaceGid(workspaceGid);
        connection.setAsanaUserGid(asanaUserGid);
        connection.setTenantId(tenantId);
        connection.setKeycloakRealm(realm);
        connection.setKeycloakUserId(keycloakUserId);

        boolean offline = tokenResponse.getScope() != null
                && tokenResponse.getScope().contains("offline_access");
        if (offline) {
            connection.setTokenType(TokenType.OFFLINE);
            connection.setOfflineTokenEnc(tokenEncryptionService.encrypt(tokenResponse.getRefreshToken()));
            connection.setRefreshTokenEnc(null);
        } else {
            connection.setTokenType(TokenType.REFRESH);
            connection.setRefreshTokenEnc(tokenEncryptionService.encrypt(tokenResponse.getRefreshToken()));
            connection.setOfflineTokenEnc(null);
        }
        connection.setScope(tokenResponse.getScope());
        connection.setStatus(ConnectionStatus.LINKED);
        return asanaConnectionRepository.save(connection);
    }

    public String decryptRenewableToken(AsanaConnection connection) {
        String encryptedToken = connection.getTokenType() == TokenType.OFFLINE
                ? connection.getOfflineTokenEnc()
                : connection.getRefreshTokenEnc();
        if (encryptedToken == null || encryptedToken.isBlank()) {
            throw new IllegalStateException("Connection has no stored renewable token");
        }
        return tokenEncryptionService.decrypt(encryptedToken);
    }

    @Transactional
    public void markNeedsReauth(AsanaConnection connection) {
        connection.setStatus(ConnectionStatus.NEEDS_REAUTH);
        asanaConnectionRepository.save(connection);
    }

    @Transactional
    public void markLastUsed(AsanaConnection connection) {
        connection.setLastUsedAt(Instant.now());
        connection.setStatus(ConnectionStatus.LINKED);
        asanaConnectionRepository.save(connection);
    }

    @Transactional
    public void rotateRenewableToken(AsanaConnection connection, KeycloakTokenResponse refreshedTokenResponse) {
        boolean offline = connection.getTokenType() == TokenType.OFFLINE
                || (refreshedTokenResponse.getScope() != null
                && refreshedTokenResponse.getScope().contains("offline_access"));

        if (offline) {
            connection.setTokenType(TokenType.OFFLINE);
            connection.setOfflineTokenEnc(tokenEncryptionService.encrypt(refreshedTokenResponse.getRefreshToken()));
            connection.setRefreshTokenEnc(null);
        } else {
            connection.setTokenType(TokenType.REFRESH);
            connection.setRefreshTokenEnc(tokenEncryptionService.encrypt(refreshedTokenResponse.getRefreshToken()));
            connection.setOfflineTokenEnc(null);
        }
        connection.setScope(refreshedTokenResponse.getScope());
        asanaConnectionRepository.save(connection);
    }
}
