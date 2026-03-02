package com.baker.integration.asana.model.assets;

public class UploadCompletedRequest {
    private String key;

    public UploadCompletedRequest() {}
    public UploadCompletedRequest(String key) { this.key = key; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
