package io.boomerang.v4.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.model.CurrentQuotas;

public class Team extends TeamEntity {

  private List<User> users;
  private List<String> workflowRefs;
  private CurrentQuotas quotas;
  
  public Team() {
    
  }
  
  public Team(TeamEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public List<User> getUserRefs() {
    return users;
  }

  public void setUserRefs(List<User> users) {
    this.users = users;
  }

  public List<String> getWorkflowRefs() {
    return workflowRefs;
  }

  public void setWorkflowRefs(List<String> workflowRefs) {
    this.workflowRefs = workflowRefs;
  }

  public CurrentQuotas getQuotas() {
    return quotas;
  }

  public void setQuotas(CurrentQuotas quotas) {
    this.quotas = quotas;
  }
}
