package net.boomerangplatform.model;

import net.boomerangplatform.mongo.entity.ApprovalEntity;

public class Approval extends ApprovalEntity {
  
  private String taskName;
  private String workflowName;
  private String teamName;
  
  public String getTaskName() {
    return taskName;
  }
  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }
  public String getWorkflowName() {
    return workflowName;
  }
  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
  public String getTeamName() {
    return teamName;
  }
  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

}
