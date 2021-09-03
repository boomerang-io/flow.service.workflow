package io.boomerang.model;

import java.util.List;
import io.boomerang.mongo.entity.TeamEntity;

public class FlowTeam extends TeamEntity {

  private List<FlowUser> users;
  private List<WorkflowSummary> workflows;

  public List<FlowUser> getUsers() {
    return users;
  }

  public void setUsers(List<FlowUser> users) {
    this.users = users;
  }

  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }


}
