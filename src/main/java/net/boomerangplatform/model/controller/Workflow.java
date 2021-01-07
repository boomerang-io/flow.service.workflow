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

  private Map<String, String> parameters;

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

  public Map<String, String> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }
}
