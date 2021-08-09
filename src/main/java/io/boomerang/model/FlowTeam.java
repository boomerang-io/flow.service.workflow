package io.boomerang.model;

import java.util.List;
import io.boomerang.mongo.entity.FlowTeamEntity;
import io.boomerang.mongo.entity.FlowUserEntity;

public class FlowTeam extends FlowTeamEntity {

  private List<FlowUserEntity> users;
  private List<WorkflowSummary> workflows;

  public List<FlowUserEntity> getUsers() {
    return users;
  }

  public void setUsers(List<FlowUserEntity> users) {
    this.users = users;
  }

  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }


}
