package net.boomerangplatform.model;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.boomerangplatform.model.controller.TaskWorkspace;

public class RequestFlowExecution {

  private boolean applyQuotas;
  
  private String token;
  
  private String workflowId;

  @JsonProperty("workspaces")
  private List<TaskWorkspace> taskWorkspaces;

  public List<TaskWorkspace> getTaskWorkspaces() {
    return taskWorkspaces;
  }

  public void setTaskWorkspaces(List<TaskWorkspace> taskWorkspaces) {
    this.taskWorkspaces = taskWorkspaces;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  private Map<String, String> properties;

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public boolean isApplyQuotas() {
    return applyQuotas;
  }

  public void setApplyQuotas(boolean applyQuotas) {
    this.applyQuotas = applyQuotas;
  }

}
