package io.boomerang.v4.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.boomerang.v4.model.ref.RunParam;

public class CanvasNodeData {
  
  String name;
  List<RunParam> params;
  String templateRef;
  Integer templateVersion;
  boolean templateUpgradesAvailable;
  
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();
  
  public String getName() {
    return name;
  }

  public void setName(String label) {
    this.name = label;
  }

  public List<RunParam> getParams() {
    return params;
  }

  public void setParams(List<RunParam> params) {
    this.params = params;
  }
  
  public String getTemplateRef() {
    return templateRef;
  }

  public void setTemplateRef(String templateRef) {
    this.templateRef = templateRef;
  }
  
  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(Integer templateVersion) {
    this.templateVersion = templateVersion;
  }

  public boolean getTemplateUpgradesAvailable() {
    return templateUpgradesAvailable;
  }

  public void setTemplateUpgradesAvailable(boolean templateUpgradesAvailable) {
    this.templateUpgradesAvailable = templateUpgradesAvailable;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }
}
