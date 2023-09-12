package io.boomerang.model.ref;

public class WorkflowRunSubmitRequest extends WorkflowRunRequest {
  
  private String workflowRef;
  
  private Integer workflowVersion;
  
  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

  public Integer getWorkflowVersion() {
    return workflowVersion;
  }

  public void setWorkflowVersion(Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
  }
}
