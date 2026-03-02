package com.baker.integration.asana.model.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadLinkResponse {
    private String uploadLink;
    private String objectKey;
    private Long exp;

    public String getUploadLink() { return uploadLink; }
    public void setUploadLink(String uploadLink) { this.uploadLink = uploadLink; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public Long getExp() { return exp; }
    public void setExp(Long exp) { this.exp = exp; }
}
