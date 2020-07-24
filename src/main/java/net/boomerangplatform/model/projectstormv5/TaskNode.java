
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
@JsonPropertyOrder({"nodeId", "type", "selected", "x", "y", "extras", "ports", "passedName",
    "taskId", "taskName"})
public class TaskNode {

  @JsonProperty("id")
  private String primaryId;

  @JsonProperty("nodeId")
  private String nodeId;
  @JsonProperty("type")
  private String type;
  @JsonProperty("selected")
  private Boolean selected;
  @JsonProperty("x")
  private Double x;
  @JsonProperty("y")
  private Double y;
  @JsonProperty("extras")
  private ExtrasNode extras;
  @JsonProperty("ports")
  private List<Port> ports = null;
  @JsonProperty("passedName")
  private String passedName;
  @JsonProperty("taskId")
  private String taskId;
  @JsonProperty("taskName")
  private String taskName;

  public void setAdditionalProperties(Map<String, Object> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  private boolean templateUpgradeAvailable;

  @JsonIgnore
  private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("nodeId")
  public String getNodeId() {
    return nodeId;
  }

  @JsonProperty("nodeId")
  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
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

  @JsonProperty("extras")
  public ExtrasNode getExtras() {
    return extras;
  }

  @JsonProperty("extras")
  public void setExtras(ExtrasNode extras) {
    this.extras = extras;
  }

  @JsonProperty("ports")
  public List<Port> getPorts() {
    return ports;
  }

  @JsonProperty("ports")
  public void setPorts(List<Port> ports) {
    this.ports = ports;
  }

  @JsonProperty("passedName")
  public String getPassedName() {
    return passedName;
  }

  @JsonProperty("passedName")
  public void setPassedName(String passedName) {
    this.passedName = passedName;
  }

  @JsonProperty("taskId")
  public String getTaskId() {
    return taskId;
  }

  @JsonProperty("taskId")
  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  @JsonProperty("taskName")
  public String getTaskName() {
    return taskName;
  }

  @JsonProperty("taskName")
  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  @JsonProperty("id")
  public String getPrimaryId() {
    return primaryId;
  }

  public void setPrimaryId(String primaryId) {
    this.primaryId = primaryId;
  }

  public boolean isTemplateUpgradeAvailable() {
    return templateUpgradeAvailable;
  }

  public void setTemplateUpgradeAvailable(boolean templateUpgradeAvailable) {
    this.templateUpgradeAvailable = templateUpgradeAvailable;
  }
}
