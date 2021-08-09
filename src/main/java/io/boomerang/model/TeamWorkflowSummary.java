package io.boomerang.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.mongo.entity.FlowTeamEntity;

public class TeamWorkflowSummary extends FlowTeamEntity {

  private List<WorkflowSummary> workflows;
  
  private WorkflowQuotas workflowQuotas;

  public TeamWorkflowSummary(FlowTeamEntity teamEntity, List<WorkflowSummary> workflows) {
    BeanUtils.copyProperties(teamEntity, this);

    this.workflows = workflows;
  }

  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowSummary> workflowSummary) {
    this.workflows = workflowSummary;
  }

  public WorkflowQuotas getWorkflowQuotas() {
    return workflowQuotas;
  }

  public void setWorkflowQuotas(WorkflowQuotas workflowQuotas) {
    this.workflowQuotas = workflowQuotas;
  }

}
