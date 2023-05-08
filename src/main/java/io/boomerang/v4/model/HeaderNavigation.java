
package io.boomerang.v4.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "url", "isDropdown", "adminOnly", "options"})
public class HeaderNavigation {

  @JsonIgnore
  private final Map<String, Object> additionalProperties = new HashMap<>();

  private Boolean isDropdown;
  private String name;
  private List<HeaderOption> options;
  private String url;

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  public Boolean getIsDropdown() {
    return isDropdown;
  }

  public String getName() {
    return name;
  }

  public List<HeaderOption> getOptions() {
    return options;
  }

  public String getUrl() {
    return url;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public void setIsDropdown(Boolean dropDown) {
    this.isDropdown = dropDown;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setOptions(List<HeaderOption> options) {
    this.options = options;
  }

  public void setUrl(String url) {
    this.url = url;
  }

}
