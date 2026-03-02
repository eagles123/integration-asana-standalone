package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaAttachmentListResponse {
    private List<AsanaAttachment> data;
    public List<AsanaAttachment> getData() { return data; }
    public void setData(List<AsanaAttachment> data) { this.data = data; }
}
