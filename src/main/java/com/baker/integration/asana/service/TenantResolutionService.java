package com.baker.integration.asana.service;

import com.baker.integration.asana.config.TenantMappingProperties;
import com.baker.integration.asana.model.connection.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class TenantResolutionService {

    private final TenantMappingProperties tenantMappingProperties;

    public TenantResolutionService(TenantMappingProperties tenantMappingProperties) {
        this.tenantMappingProperties = tenantMappingProperties;
    }

    public TenantContext resolve(String workspaceGid) {
        String tenantId = tenantMappingProperties.getWorkspaceTenantMap().get(workspaceGid);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = tenantMappingProperties.getDefaultTenantId();
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("No tenant mapping found for workspace: " + workspaceGid);
        }

        String realm = tenantMappingProperties.getTenantRealmMap().get(tenantId);
        if (realm == null || realm.isBlank()) {
            realm = tenantMappingProperties.getDefaultRealm();
        }
        if (realm == null || realm.isBlank()) {
            throw new IllegalArgumentException("No Keycloak realm mapping found for tenant: " + tenantId);
        }

        return new TenantContext(workspaceGid, tenantId, realm);
    }
}
