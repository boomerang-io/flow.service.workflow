package io.boomerang.tekton;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.v4.model.enums.ref.ParamType;

public class Param {
  private String name;
  private ParamType type;
  private String description;
  @JsonProperty("default")
  private Object defaultValue;
 
  private Map<String, Object> unknownFields = new HashMap<>();

  @JsonAnyGetter
  @JsonPropertyOrder(alphabetic = true)
  public Map<String, Object> otherFields() {
    return unknownFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, Object value) {
    unknownFields.put(name, value);
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ParamType getType() {
    return type;
  }

  public void setType(ParamType type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(Object object) {
    this.defaultValue = object;
  }
}
