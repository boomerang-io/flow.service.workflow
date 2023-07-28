package io.boomerang.v4.model;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.model.enums.TeamStatus;

public class TeamSummary {

  private String id;
  private String name;
  private Date creationDate = new Date();
  private TeamStatus status = TeamStatus.active;
  private String externalRef;
  private Map<String, String> labels = new HashMap<>();
  private List<UserSummary> members;
  private List<String> workflows = new LinkedList<>();
  
  public TeamSummary() {
    
  }
  
  public TeamSummary(Team entity) {
    BeanUtils.copyProperties(entity, this);
  }

  
  public TeamSummary(TeamEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public TeamStatus getStatus() {
    return status;
  }

  public void setStatus(TeamStatus status) {
    this.status = status;
  }

  public String getExternalRef() {
    return externalRef;
  }

  public void setExternalRef(String externalRef) {
    this.externalRef = externalRef;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public List<String> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<String> workflows) {
    this.workflows = workflows;
  }

  public List<UserSummary> getMembers() {
    return members;
  }

  public void setMembers(List<UserSummary> members) {
    this.members = members;
  }
}
