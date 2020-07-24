
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

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"type", "id", "selected", "source", "sourcePort", "target", "targetPort",
    "points", "extras", "labels", "width", "color", "curvyness", "executionCondition"})
public class Link {

  private String linkId;

  @JsonProperty("type")
  private String type;
  @JsonProperty("id")
  private String id;
  @JsonProperty("selected")
  private boolean selected;
  @JsonProperty("source")
  private String source;
  @JsonProperty("sourcePort")
  private String sourcePort;
  @JsonProperty("target")
  private String target;
  @JsonProperty("targetPort")
  private String targetPort;
  @JsonProperty("points")
  private List<Point> points = null;
  @JsonProperty("extras")
  private Extras extras;
  @JsonProperty("labels")
  private List<Object> labels = null;
  @JsonProperty("width")
  private Integer width;
  @JsonProperty("color")
  private String color;
  @JsonProperty("curvyness")
  private Integer curvyness;
  @JsonProperty("executionCondition")
  private String executionCondition;
  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  private String switchCondition;

  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("selected")
  public boolean getSelected() {
    return selected;
  }

  @JsonProperty("selected")
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  @JsonProperty("source")
  public String getSource() {
    return source;
  }

  @JsonProperty("source")
  public void setSource(String source) {
    this.source = source;
  }

  @JsonProperty("sourcePort")
  public String getSourcePort() {
    return sourcePort;
  }

  @JsonProperty("sourcePort")
  public void setSourcePort(String sourcePort) {
    this.sourcePort = sourcePort;
  }

  @JsonProperty("target")
  public String getTarget() {
    return target;
  }

  @JsonProperty("target")
  public void setTarget(String target) {
    this.target = target;
  }

  @JsonProperty("targetPort")
  public String getTargetPort() {
    return targetPort;
  }

  @JsonProperty("targetPort")
  public void setTargetPort(String targetPort) {
    this.targetPort = targetPort;
  }

  @JsonProperty("points")
  public List<Point> getPoints() {
    return points;
  }

  @JsonProperty("points")
  public void setPoints(List<Point> points) {
    this.points = points;
  }

  @JsonProperty("extras")
  public Extras getExtras() {
    return extras;
  }

  @JsonProperty("extras")
  public void setExtras(Extras extras) {
    this.extras = extras;
  }

  @JsonProperty("labels")
  public List<Object> getLabels() {
    return labels;
  }

  @JsonProperty("labels")
  public void setLabels(List<Object> labels) {
    this.labels = labels;
  }

  @JsonProperty("width")
  public Integer getWidth() {
    return width;
  }

  @JsonProperty("width")
  public void setWidth(Integer width) {
    this.width = width;
  }

  @JsonProperty("color")
  public String getColor() {
    return color;
  }

  @JsonProperty("color")
  public void setColor(String color) {
    this.color = color;
  }

  @JsonProperty("curvyness")
  public Integer getCurvyness() {
    return curvyness;
  }

  @JsonProperty("curvyness")
  public void setCurvyness(Integer curvyness) {
    this.curvyness = curvyness;
  }

  @JsonProperty("executionCondition")
  public String getExecutionCondition() {
    return executionCondition;
  }

  @JsonProperty("executionCondition")
  public void setExecutionCondition(String executionCondition) {
    this.executionCondition = executionCondition;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  public String getLinkId() {
    return linkId;
  }

  public void setLinkId(String linkId) {
    this.linkId = linkId;
  }

  public String getSwitchCondition() {
    return switchCondition;
  }

  public void setSwitchCondition(String switchCondition) {
    this.switchCondition = switchCondition;
  }

}
