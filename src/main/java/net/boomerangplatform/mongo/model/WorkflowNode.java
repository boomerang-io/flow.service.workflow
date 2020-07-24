package net.boomerangplatform.mongo.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class WorkflowNode {

  private String nodeId;
  private String taskId;
  private TaskType type;
  private Boolean selected;
  private Double x;
  private Double y;
  private NodeExtras extras;
  private List<NodePort> ports;
  private String passedName;
  private String taskName;

  public WorkflowNode() {
    // Do nothing
  }

  public String getNodeId() {
    return nodeId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public TaskType getType() {
    return type;
  }

  public void setType(TaskType type) {
    this.type = type;
  }

  public Boolean getSelected() {
    return selected;
  }

  public void setSelected(Boolean selected) {
    this.selected = selected;
  }

  public Double getX() {
    return x;
  }

  public void setX(Double x) {
    this.x = x;
  }

  public Double getY() {
    return y;
  }

  public void setY(Double y) {
    this.y = y;
  }

  public NodeExtras getExtras() {
    return extras;
  }

  public void setExtras(NodeExtras extras) {
    this.extras = extras;
  }

  public List<NodePort> getPorts() {
    return ports;
  }

  public void setPorts(List<NodePort> ports) {
    this.ports = ports;
  }

  public String getPassedName() {
    return passedName;
  }

  public void setPassedName(String passedName) {
    this.passedName = passedName;
  }

  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }
}
