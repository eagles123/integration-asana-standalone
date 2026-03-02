package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaAttachment {
    private String gid;
    private String name;
    @JsonProperty("download_url") private String downloadUrl;
    @JsonProperty("content_type") private String contentType;
    private Long size;
    private String host;
    @JsonProperty("view_url") private String viewUrl;

    public String getGid() { return gid; }
    public void setGid(String gid) { this.gid = gid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public String getViewUrl() { return viewUrl; }
    public void setViewUrl(String viewUrl) { this.viewUrl = viewUrl; }
}
