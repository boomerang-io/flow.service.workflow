package io.boomerang.model;

import java.util.List;
import io.boomerang.mongo.entity.FlowUserEntity;
import io.boomerang.mongo.entity.TeamEntity;

public class FlowUserProfile extends FlowUserEntity {

  private List<WorkflowSummary> workflows;
  private List<TeamEntity> userTeams;

  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }

  public List<TeamEntity> getUserTeams() {
    return userTeams;
  }

  public void setUserTeams(List<TeamEntity> userTeams) {
    this.userTeams = userTeams;
  }

}
