
package net.boomerangplatform.model.projectstormv5;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "selected", "x", "y"})
public class Point {

  @JsonProperty("id")
  private String id;
  @JsonProperty("selected")
  private Boolean selected;
  @JsonProperty("x")
  private Double x;
  @JsonProperty("y")
  private Double y;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("selected")
  public Boolean getSelected() {
    return selected;
  }

  @JsonProperty("selected")
  public void setSelected(Boolean selected) {
    this.selected = selected;
  }

  @JsonProperty("x")
  public Double getX() {
    return x;
  }

  @JsonProperty("x")
  public void setX(Double x) {
    this.x = x;
  }

  @JsonProperty("y")
  public Double getY() {
    return y;
  }

  @JsonProperty("y")
  public void setY(Double y) {
    this.y = y;
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
