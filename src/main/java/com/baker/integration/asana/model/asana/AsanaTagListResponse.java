package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaTagListResponse {
    private List<AsanaTag> data;
    public List<AsanaTag> getData() { return data; }
    public void setData(List<AsanaTag> data) { this.data = data; }
}
