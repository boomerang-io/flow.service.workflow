package io.boomerang.model;

import io.boomerang.mongo.model.WorkflowScope;

public class DuplicateRequest {
  public WorkflowScope getScope() {
    return scope;
  }
  public void setScope(WorkflowScope scope) {
    this.scope = scope;
  }
  public String getTeamId() {
    return teamId;
  }
  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public String getSummary() {
    return summary;
  }
  public void setSummary(String summary) {
    this.summary = summary;
  }
  public String getIcon() {
    return icon;
  }
  public void setIcon(String icon) {
    this.icon = icon;
  }
  private WorkflowScope scope;
  private String teamId;
  private String name;
  private String description;
  private String summary;
  private String icon;

}
