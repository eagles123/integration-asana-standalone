package com.baker.integration.asana.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "lytho-auth")
public class LythoAuthProperties {

    private String keycloakBaseUrl = "https://login.us-1.golytho.us/auth";
    private String realm = "qaorange";
    private String redirectUri = "https://auth.us-1.golytho.us/callback";
    private String clientId = "external-integration";
    private String clientSecret = "IKA5RJFjXshiaCUEFhbdOJzlfLxwj6Vy";
    private String asanaClientId = "1213488390127334";
    private String asanaClientSecret = "a96229b124b57b43f946c78ac2b1ef72";
    private String scope = "openid profile email";

    public String getKeycloakBaseUrl() {
        return keycloakBaseUrl;
    }

    public void setKeycloakBaseUrl(String keycloakBaseUrl) {
        this.keycloakBaseUrl = keycloakBaseUrl;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAsanaClientId() {
        return asanaClientId;
    }

    public void setAsanaClientId(String asanaClientId) {
        this.asanaClientId = asanaClientId;
    }

    public String getAsanaClientSecret() {
        return asanaClientSecret;
    }

    public void setAsanaClientSecret(String asanaClientSecret) {
        this.asanaClientSecret = asanaClientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
}
