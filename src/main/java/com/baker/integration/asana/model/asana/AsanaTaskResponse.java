package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaTaskResponse {
    private AsanaTask data;
    public AsanaTask getData() { return data; }
    public void setData(AsanaTask data) { this.data = data; }
}
