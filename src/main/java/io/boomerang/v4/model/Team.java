package io.boomerang.v4.model;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import io.boomerang.v4.data.entity.TeamEntity;
import io.boomerang.v4.data.model.CurrentQuotas;
import io.boomerang.v4.model.enums.TeamStatus;

public class Team {

  private String id;
  private String name;
  private Date creationDate = new Date();
  private TeamStatus status = TeamStatus.active;
  private String externalRef;
  private Map<String, String> labels = new HashMap<>();
  private List<AbstractParam> parameters = new LinkedList<>();
//  private TeamSettings settings;
  private CurrentQuotas quotas;
  private List<UserSummary> members;
  private List<WorkflowSummary> workflows = new LinkedList<>();
  private List<ApproverGroup> approverGroups;
  
  public Team() {
    
  }
  
  public Team(TeamEntity entity) {
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

  public List<AbstractParam> getParameters() {
    return parameters;
  }

  public void setParameters(List<AbstractParam> parameters) {
    this.parameters = parameters;
  }

//  public TeamSettings getSettings() {
//    return settings;
//  }
//
//  public void setSettings(TeamSettings settings) {
//    this.settings = settings;
//  }

  public CurrentQuotas getQuotas() {
    return quotas;
  }

  public void setQuotas(CurrentQuotas quotas) {
    this.quotas = quotas;
  }

  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }

  public List<UserSummary> getMembers() {
    return members;
  }

  public void setMembers(List<UserSummary> members) {
    this.members = members;
  }

  public List<ApproverGroup> getApproverGroups() {
    return approverGroups;
  }

  public void setApproverGroups(List<ApproverGroup> approverGroups) {
    this.approverGroups = approverGroups;
  }
}
