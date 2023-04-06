package io.boomerang.v4.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.model.CurrentQuotas;

public class Team extends TeamEntity {

  private List<UserSummary> users;
  private List<String> workflowRefs;
  private List<ApproverGroup> approverGroups;
  private CurrentQuotas quotas;
  
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

  public List<String> getWorkflowRefs() {
    return workflowRefs;
  }

  public void setWorkflowRefs(List<String> workflowRefs) {
    this.workflowRefs = workflowRefs;
  }

  public List<ApproverGroup> getApproverGroups() {
    return approverGroups;
  }

  public void setApproverGroups(List<ApproverGroup> approverGroups) {
    this.approverGroups = approverGroups;
  }

  public CurrentQuotas getQuotas() {
    return quotas;
  }

  public void setQuotas(CurrentQuotas quotas) {
    this.quotas = quotas;
  }
}
