package io.boomerang.model;

import java.util.List;

public class UserWorkflowSummary {

  private WorkflowQuotas userQuotas;
  private List<WorkflowSummary> workflows;
  
  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }
  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }
  public WorkflowQuotas getUserQuotas() {
    return userQuotas;
  }
  public void setUserQuotas(WorkflowQuotas userQuotas) {
    this.userQuotas = userQuotas;
  }
  
}
