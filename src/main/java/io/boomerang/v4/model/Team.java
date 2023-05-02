package io.boomerang.v4.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.model.CurrentQuotas;

public class Team extends TeamEntity {

  private List<UserSummary> users;
  private List<WorkflowSummary> workflowSummary;
  private List<ApproverGroup> approverGroups;
  private CurrentQuotas currentQuotas;
  
  public Team() {
    
  }
  
  public Team(TeamEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public List<UserSummary> getUsers() {
    return users;
  }

  public void setUsers(List<UserSummary> users) {
    this.users = users;
  }

  public List<WorkflowSummary> getWorkflowSummary() {
    return workflowSummary;
  }

  public void setWorkflowSummary(List<WorkflowSummary> workflowSummary) {
    this.workflowSummary = workflowSummary;
  }

  public List<ApproverGroup> getApproverGroups() {
    return approverGroups;
  }

  public void setApproverGroups(List<ApproverGroup> approverGroups) {
    this.approverGroups = approverGroups;
  }

  public CurrentQuotas getQuotas() {
    return currentQuotas;
  }

  public void setQuotas(CurrentQuotas quotas) {
    this.currentQuotas = quotas;
  }
}
