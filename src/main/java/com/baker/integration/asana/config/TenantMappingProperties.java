package com.baker.integration.asana.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "tenant-mapping")
public class TenantMappingProperties {

    private Map<String, String> workspaceTenantMap = new HashMap<>();
    private Map<String, String> tenantRealmMap = new HashMap<>();

    public Map<String, String> getWorkspaceTenantMap() {
        return workspaceTenantMap;
    }

    public void setWorkspaceTenantMap(Map<String, String> workspaceTenantMap) {
        this.workspaceTenantMap = workspaceTenantMap;
    }

    public Map<String, String> getTenantRealmMap() {
        return tenantRealmMap;
    }

    public void setTenantRealmMap(Map<String, String> tenantRealmMap) {
        this.tenantRealmMap = tenantRealmMap;
    }
}
