package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaAttachmentDetailResponse {
    private AsanaAttachment data;
    public AsanaAttachment getData() { return data; }
    public void setData(AsanaAttachment data) { this.data = data; }
}
