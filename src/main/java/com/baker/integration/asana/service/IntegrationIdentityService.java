package com.baker.integration.asana.service;

import com.baker.integration.asana.config.IdentityMappingProperties;
import com.baker.integration.asana.model.integration.IntegrationIdentity;
import org.springframework.stereotype.Service;

@Service
public class IntegrationIdentityService {

    private final IdentityMappingProperties identityMappingProperties;

    public IntegrationIdentityService(IdentityMappingProperties identityMappingProperties) {
        this.identityMappingProperties = identityMappingProperties;
    }

    public IntegrationIdentity resolveIdentity(String asanaUserId, String asanaWorkspaceId) {
        if (asanaUserId == null || asanaUserId.isBlank()) {
            throw new IllegalArgumentException("Asana user id is required");
        }
        if (asanaWorkspaceId == null || asanaWorkspaceId.isBlank()) {
            throw new IllegalArgumentException("Asana workspace id is required");
        }

        String tenantId = identityMappingProperties.getWorkspaceTenantMap().get(asanaWorkspaceId);
        if (tenantId == null || tenantId.isBlank()) {
            if (identityMappingProperties.isRequireWorkspaceTenantMapping()) {
                throw new IllegalArgumentException("No tenant mapping configured for Asana workspace: " + asanaWorkspaceId);
            }
            tenantId = asanaWorkspaceId;
        }

        String damUserId = identityMappingProperties.getAsanaUserDamUserMap().get(asanaUserId);
        if (damUserId == null || damUserId.isBlank()) {
            if (identityMappingProperties.isRequireDamUserMapping()) {
                throw new IllegalArgumentException("No DAM user mapping configured for Asana user: " + asanaUserId);
            }
            damUserId = asanaUserId;
        }

        return new IntegrationIdentity(tenantId, damUserId, asanaUserId, asanaWorkspaceId);
    }
}
