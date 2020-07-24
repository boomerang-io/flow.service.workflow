
package net.boomerangplatform.model.projectstormv5;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"nodePortId", "type", "selected", "name", "parentNode", "links", "position",
    "id"})
public class Port {

  @JsonProperty("id")
  private String id;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("nodePortId")
  private String nodePortId;
  @JsonProperty("type")
  private String type;
  @JsonProperty("selected")
  private Boolean selected;
  @JsonProperty("name")
  private String name;
  @JsonProperty("parentNode")
  private String parentNode;
  @JsonProperty("links")
  private List<String> links = null;
  @JsonProperty("position")
  private String position;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("nodePortId")
  public String getNodePortId() {
    return nodePortId;
  }

  @JsonProperty("nodePortId")
  public void setNodePortId(String nodePortId) {
    this.nodePortId = nodePortId;
  }

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("selected")
  public Boolean getSelected() {
    return selected;
  }

  @JsonProperty("selected")
  public void setSelected(Boolean selected) {
    this.selected = selected;
  }

  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  @JsonProperty("parentNode")
  public String getParentNode() {
    return parentNode;
  }

  @JsonProperty("parentNode")
  public void setParentNode(String parentNode) {
    this.parentNode = parentNode;
  }

  @JsonProperty("links")
  public List<String> getLinks() {
    return links;
  }

  @JsonProperty("links")
  public void setLinks(List<String> links) {
    this.links = links;
  }

  @JsonProperty("position")
  public String getPosition() {
    return position;
  }

  @JsonProperty("position")
  public void setPosition(String position) {
    this.position = position;
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
