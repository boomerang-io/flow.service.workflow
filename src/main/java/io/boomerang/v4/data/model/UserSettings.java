package io.boomerang.v4.data.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

// Future extensibility
public class UserSettings {

  private Boolean isFirstVisit = true;
  private Boolean isShowHelp = true;
  private Boolean hasConsented = false;
  @JsonIgnore
  private final Map<String, Object> additionalProperties = new HashMap<>();

  public Boolean getIsFirstVisit() {
    return isFirstVisit;
  }
  public void setIsFirstVisit(Boolean isFirstVisit) {
    this.isFirstVisit = isFirstVisit;
  }
  public Boolean getIsShowHelp() {
    return isShowHelp;
  }
  public void setIsShowHelp(Boolean isShowHelp) {
    this.isShowHelp = isShowHelp;
  }
  public Boolean getHasConsented() {
    return hasConsented;
  }
  public void setHasConsented(Boolean hasConsented) {
    this.hasConsented = hasConsented;
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
