package com.baker.integration.asana.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "asana")
public class AsanaAppProperties {

    private String clientSecret;
    private String apiBaseUrl = "https://app.asana.com/api/1.0";
    private String personalAccessToken;

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getPersonalAccessToken() { return personalAccessToken; }
    public void setPersonalAccessToken(String personalAccessToken) { this.personalAccessToken = personalAccessToken; }
}
