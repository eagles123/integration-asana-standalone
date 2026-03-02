package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaSubmitRequest {
    private String task;
    private String user;
    private String workspace;
    private String locale;
    @JsonProperty("expires_at") private String expiresAt;
    private Map<String, Object> values;

    public String getTask() { return task; }
    public void setTask(String task) { this.task = task; }
    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }
    public String getWorkspace() { return workspace; }
    public void setWorkspace(String workspace) { this.workspace = workspace; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
    public Map<String, Object> getValues() { return values; }
    public void setValues(Map<String, Object> values) { this.values = values; }
}
