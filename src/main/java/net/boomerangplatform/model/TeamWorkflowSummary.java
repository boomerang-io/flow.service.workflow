package net.boomerangplatform.model;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;

public class TeamWorkflowSummary extends FlowTeamEntity {

  private List<WorkflowSummary> workflows;
  
  private WorkflowQuotas workflowQuotas;

  public TeamWorkflowSummary(FlowTeamEntity teamEntity, List<WorkflowSummary> workflows) {
    BeanUtils.copyProperties(teamEntity, this);
    // give an empty list even when workflows doesn't exist.
    this.workflows = workflows == null? new ArrayList<WorkflowSummary>() : workflows;
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
