package com.baker.integration.asana.model.asana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AsanaCustomField {
    private String gid;
    private String name;
    private String type;
    @JsonProperty("display_value") private String displayValue;
    @JsonProperty("text_value") private String textValue;
    @JsonProperty("number_value") private Double numberValue;
    @JsonProperty("enum_value") private EnumValue enumValue;
    @JsonProperty("multi_enum_values") private List<EnumValue> multiEnumValues;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EnumValue {
        private String gid;
        private String name;
        private String color;
        private Boolean enabled;

        public String getGid() { return gid; }
        public void setGid(String gid) { this.gid = gid; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    }

    public String getGid() { return gid; }
    public void setGid(String gid) { this.gid = gid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDisplayValue() { return displayValue; }
    public void setDisplayValue(String displayValue) { this.displayValue = displayValue; }
    public String getTextValue() { return textValue; }
    public void setTextValue(String textValue) { this.textValue = textValue; }
    public Double getNumberValue() { return numberValue; }
    public void setNumberValue(Double numberValue) { this.numberValue = numberValue; }
    public EnumValue getEnumValue() { return enumValue; }
    public void setEnumValue(EnumValue enumValue) { this.enumValue = enumValue; }
    public List<EnumValue> getMultiEnumValues() { return multiEnumValues; }
    public void setMultiEnumValues(List<EnumValue> multiEnumValues) { this.multiEnumValues = multiEnumValues; }
}
