package net.boomerangplatform.mongo.model;

import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class TaskConfigurationNode {

  private Map<String, String> inputs = new HashMap<>();

  private String nodeId;
  private String taskId;

  public TaskConfigurationNode() {
    // Do nothing
  }

  public void setInputs(Map<String, String> inputs) {
    this.inputs = inputs;
  }

  public Map<String, String> getInputs() {
    return inputs;
  }

  public String getNodeId() {
    return nodeId;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }
}
