package net.boomerangplatform.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import net.boomerangplatform.mongo.entity.FlowTeamEntity;

public class TeamWorkflowSummary extends FlowTeamEntity {

  private List<WorkflowSummary> workflows;

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

}
