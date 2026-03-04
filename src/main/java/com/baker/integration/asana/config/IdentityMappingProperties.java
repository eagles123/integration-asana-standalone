package com.baker.integration.asana.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "identity-mapping")
public class IdentityMappingProperties {

    private Map<String, String> workspaceTenantMap = new HashMap<>();
    private Map<String, String> asanaUserDamUserMap = new HashMap<>();
    private boolean requireWorkspaceTenantMapping = false;
    private boolean requireDamUserMapping = false;

    public Map<String, String> getWorkspaceTenantMap() {
        return workspaceTenantMap;
    }

    public void setWorkspaceTenantMap(Map<String, String> workspaceTenantMap) {
        this.workspaceTenantMap = workspaceTenantMap;
    }

    public Map<String, String> getAsanaUserDamUserMap() {
        return asanaUserDamUserMap;
    }

    public void setAsanaUserDamUserMap(Map<String, String> asanaUserDamUserMap) {
        this.asanaUserDamUserMap = asanaUserDamUserMap;
    }

    public boolean isRequireWorkspaceTenantMapping() {
        return requireWorkspaceTenantMapping;
    }

    public void setRequireWorkspaceTenantMapping(boolean requireWorkspaceTenantMapping) {
        this.requireWorkspaceTenantMapping = requireWorkspaceTenantMapping;
    }

    public boolean isRequireDamUserMapping() {
        return requireDamUserMapping;
    }

    public void setRequireDamUserMapping(boolean requireDamUserMapping) {
        this.requireDamUserMapping = requireDamUserMapping;
    }
}
