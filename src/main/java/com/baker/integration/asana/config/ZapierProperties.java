package com.baker.integration.asana.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "zapier")
public class ZapierProperties {

    private String catchHookUrl;
    private long timeoutSeconds = 10;

    public String getCatchHookUrl() {
        return catchHookUrl;
    }

    public void setCatchHookUrl(String catchHookUrl) {
        this.catchHookUrl = catchHookUrl;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
