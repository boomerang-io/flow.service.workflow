package net.boomerangplatform.model.controller;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class Workflow {

  private String workflowName;

  private String workflowId;

  private String workflowActivityId;

  @JsonProperty("storage")
  private WorkflowStorage storage;

  private Map<String, String> properties;

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowActivityId() {
    return workflowActivityId;
  }

  public void setWorkflowActivityId(String workflowActivityId) {
    this.workflowActivityId = workflowActivityId;
  }

  public WorkflowStorage getWorkflowStorage() {
    return storage;
  }

  public void setWorkflowStorage(WorkflowStorage storage) {
    this.storage = storage;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }
}
