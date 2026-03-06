package com.baker.integration.asana.model.connection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.Instant;

@Entity
@Table(
        name = "asana_connection",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_asana_connection_workspace_user", columnNames = {
                        "asana_workspace_gid", "asana_user_gid"
                })
        },
        indexes = {
                @Index(name = "idx_asana_connection_workspace_user", columnList = "asana_workspace_gid,asana_user_gid"),
                @Index(name = "idx_asana_connection_status", columnList = "status")
        }
)
public class AsanaConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asana_workspace_gid", nullable = false, length = 64)
    private String asanaWorkspaceGid;

    @Column(name = "asana_user_gid", nullable = false, length = 64)
    private String asanaUserGid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "keycloak_realm", nullable = false, length = 128)
    private String keycloakRealm;

    @Column(name = "keycloak_user_id", nullable = false, length = 128)
    private String keycloakUserId;

    @Column(name = "token_type", nullable = false, length = 32)
    private TokenType tokenType;

    @Column(name = "refresh_token_enc", columnDefinition = "TEXT")
    private String refreshTokenEnc;

    @Column(name = "offline_token_enc", columnDefinition = "TEXT")
    private String offlineTokenEnc;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    @Column(name = "status", nullable = false, length = 32)
    private ConnectionStatus status;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
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

    public String getKeycloakRealm() {
        return keycloakRealm;
    }

    public void setKeycloakRealm(String keycloakRealm) {
        this.keycloakRealm = keycloakRealm;
    }

    public String getKeycloakUserId() {
        return keycloakUserId;
    }

    public void setKeycloakUserId(String keycloakUserId) {
        this.keycloakUserId = keycloakUserId;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getRefreshTokenEnc() {
        return refreshTokenEnc;
    }

    public void setRefreshTokenEnc(String refreshTokenEnc) {
        this.refreshTokenEnc = refreshTokenEnc;
    }

    public String getOfflineTokenEnc() {
        return offlineTokenEnc;
    }

    public void setOfflineTokenEnc(String offlineTokenEnc) {
        this.offlineTokenEnc = offlineTokenEnc;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
