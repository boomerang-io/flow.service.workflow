package io.boomerang.model;

import java.util.List;

public class UserWorkflowSummary {

  private WorkflowQuotas workflowQuotas;
  private List<WorkflowSummary> workflows;
  
  public WorkflowQuotas getWorkflowQuotas() {
    return workflowQuotas;
  }
  public void setWorkflowQuotas(WorkflowQuotas workflowQuotas) {
    this.workflowQuotas = workflowQuotas;
  }
  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }
  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }
  
}
