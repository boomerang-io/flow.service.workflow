
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
@JsonPropertyOrder({"gridSize", "links", "nodes", "offsetX", "offsetY", "zoom"})
public class RestDag {

  private String id;

  @JsonProperty("gridSize")
  private Integer gridSize;
  @JsonProperty("links")
  private List<Link> links = null;
  @JsonProperty("nodes")
  private List<TaskNode> nodes = null;
  @JsonProperty("offsetX")
  private Integer offsetX;
  @JsonProperty("offsetY")
  private Integer offsetY;
  @JsonProperty("zoom")
  private Integer zoom;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("gridSize")
  public Integer getGridSize() {
    return gridSize;
  }

  @JsonProperty("gridSize")
  public void setGridSize(Integer gridSize) {
    this.gridSize = gridSize;
  }

  @JsonProperty("links")
  public List<Link> getLinks() {
    return links;
  }

  @JsonProperty("links")
  public void setLinks(List<Link> links) {
    this.links = links;
  }

  @JsonProperty("nodes")
  public List<TaskNode> getNodes() {
    return nodes;
  }

  @JsonProperty("nodes")
  public void setNodes(List<TaskNode> nodes) {
    this.nodes = nodes;
  }

  @JsonProperty("offsetX")
  public Integer getOffsetX() {
    return offsetX;
  }

  @JsonProperty("offsetX")
  public void setOffsetX(Integer offsetX) {
    this.offsetX = offsetX;
  }

  @JsonProperty("offsetY")
  public Integer getOffsetY() {
    return offsetY;
  }

  @JsonProperty("offsetY")
  public void setOffsetY(Integer offsetY) {
    this.offsetY = offsetY;
  }

  @JsonProperty("zoom")
  public Integer getZoom() {
    return zoom;
  }

  @JsonProperty("zoom")
  public void setZoom(Integer zoom) {
    this.zoom = zoom;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

}
