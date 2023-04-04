package io.boomerang.security.model;

import io.boomerang.mongo.model.TokenScope;

public class WorkflowToken extends Token {

  private String workflowRef;
  
  public WorkflowToken() {
    this.setScope(TokenScope.workflow);
  }
  
  public WorkflowToken(String workflowRef) {
    super();
    this.workflowRef = workflowRef;
    this.setScope(TokenScope.workflow);
  }

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }
}
