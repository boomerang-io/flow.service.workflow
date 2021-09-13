package io.boomerang.model.teams;

import io.boomerang.mongo.entity.ApprovalEntity;
import io.boomerang.mongo.model.WorkflowScope;

public class Action extends ApprovalEntity {
  
  private String taskName;
  private String workflowName;
  private String teamName;
  private String instructions;
  private WorkflowScope scope;
  private long numberOfApprovals;
  private long approvalsRequired;
  
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
  public String getInstructions() {
    return instructions;
  }
  public void setInstructions(String instructions) {
    this.instructions = instructions;
  }
  public WorkflowScope getScope() {
    return scope;
  }
  public void setScope(WorkflowScope scope) {
    this.scope = scope;
  }
  public long getApprovalsRequired() {
    return approvalsRequired;
  }
  public void setApprovalsRequired(long approvalsRequired) {
    this.approvalsRequired = approvalsRequired;
  }
  public long getNumberOfApprovals() {
    return numberOfApprovals;
  }
  public void setNumberOfApprovals(long numberOfApprovals) {
    this.numberOfApprovals = numberOfApprovals;
  }

}
