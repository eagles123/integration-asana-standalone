package com.baker.integration.asana.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "link-flow")
public class LinkFlowProperties {

    private String stateSigningSecret;
    private long stateTtlSeconds = 600;
    private String tokenEncryptionKey;

    public String getStateSigningSecret() {
        return stateSigningSecret;
    }

    public void setStateSigningSecret(String stateSigningSecret) {
        this.stateSigningSecret = stateSigningSecret;
    }

    public long getStateTtlSeconds() {
        return stateTtlSeconds;
    }

    public void setStateTtlSeconds(long stateTtlSeconds) {
        this.stateTtlSeconds = stateTtlSeconds;
    }

    public String getTokenEncryptionKey() {
        return tokenEncryptionKey;
    }

    public void setTokenEncryptionKey(String tokenEncryptionKey) {
        this.tokenEncryptionKey = tokenEncryptionKey;
    }
}
