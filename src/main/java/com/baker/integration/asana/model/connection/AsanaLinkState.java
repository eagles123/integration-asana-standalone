package com.baker.integration.asana.model.connection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "asana_link_state",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_asana_link_state_nonce", columnNames = {"state_nonce"})
        },
        indexes = {
                @Index(name = "idx_asana_link_state_expires", columnList = "expires_at"),
                @Index(name = "idx_asana_link_state_workspace_user", columnList = "asana_workspace_gid,asana_user_gid")
        }
)
public class AsanaLinkState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_nonce", nullable = false, length = 128)
    private String stateNonce;

    @Column(name = "asana_workspace_gid", nullable = false, length = 64)
    private String asanaWorkspaceGid;

    @Column(name = "asana_user_gid", nullable = false, length = 64)
    private String asanaUserGid;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getStateNonce() {
        return stateNonce;
    }

    public void setStateNonce(String stateNonce) {
        this.stateNonce = stateNonce;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
