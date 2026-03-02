package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaTask {
    private String gid;
    private String name;
    @JsonProperty("custom_fields") private List<AsanaCustomField> customFields;

    public String getGid() { return gid; }
    public void setGid(String gid) { this.gid = gid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<AsanaCustomField> getCustomFields() { return customFields; }
    public void setCustomFields(List<AsanaCustomField> customFields) { this.customFields = customFields; }
}
