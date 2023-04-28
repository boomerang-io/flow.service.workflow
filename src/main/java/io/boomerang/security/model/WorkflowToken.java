package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class WorkflowToken extends Token {
  
  private String workflowId;
  
  public WorkflowToken(String workflowId) {
    super();
    this.setScope(TokenScope.team);
    this.workflowId = workflowId;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
}
