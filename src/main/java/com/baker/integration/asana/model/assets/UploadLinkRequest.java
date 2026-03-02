package com.baker.integration.asana.model.assets;

public class UploadLinkRequest {
    private String fileName;
    private String contentType;

    public UploadLinkRequest() {}
    public UploadLinkRequest(String fileName, String contentType) {
        this.fileName = fileName;
        this.contentType = contentType;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}
