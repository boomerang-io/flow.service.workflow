package io.boomerang.model;

import java.util.List;
import io.boomerang.v4.data.model.CurrentQuotas;

public class UserWorkflowSummary {

  private CurrentQuotas userQuotas;
  private List<WorkflowSummary> workflows;
  
  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }
  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }
  public CurrentQuotas getUserQuotas() {
    return userQuotas;
  }
  public void setUserQuotas(CurrentQuotas userQuotas) {
    this.userQuotas = userQuotas;
  }
  
}
