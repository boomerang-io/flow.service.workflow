package net.boomerangplatform.mongo.model;

import java.util.Date;

public class FlowTeamQuotas {

  private Integer maxWorkflowCount;
  private Integer maxWorkflowExecutionMonthly;
  private Integer maxWorkflowStorage;
  private Integer maxWorkflowExecutionTime;
  private Integer maxConcurrentWorkflows;
  private Integer currentWorkflowCount;
  private Integer currentConcurrentWorkflows;
  private Integer currentWorkflowExecutionMonthly;
  private Date resetDate;
  
  public Integer getMaxWorkflowCount() {
    return maxWorkflowCount;
  }
  public void setMaxWorkflowCount(Integer maxWorkflowCount) {
    this.maxWorkflowCount = maxWorkflowCount;
  }
  public Integer getMaxWorkflowExecutionMonthly() {
    return maxWorkflowExecutionMonthly;
  }
  public void setMaxWorkflowExecutionMonthly(Integer maxWorkflowExecutionMonthly) {
    this.maxWorkflowExecutionMonthly = maxWorkflowExecutionMonthly;
  }
  public Integer getMaxWorkflowStorage() {
    return maxWorkflowStorage;
  }
  public void setMaxWorkflowStorage(Integer maxWorkflowStorage) {
    this.maxWorkflowStorage = maxWorkflowStorage;
  }
  public Integer getMaxWorkflowExecutionTime() {
    return maxWorkflowExecutionTime;
  }
  public void setMaxWorkflowExecutionTime(Integer maxWorkflowExecutionTime) {
    this.maxWorkflowExecutionTime = maxWorkflowExecutionTime;
  }
  public Integer getMaxConcurrentWorkflows() {
    return maxConcurrentWorkflows;
  }
  public void setMaxConcurrentWorkflows(Integer maxConcurrentWorkflows) {
    this.maxConcurrentWorkflows = maxConcurrentWorkflows;
  }
  public Integer getCurrentWorkflowCount() {
    return currentWorkflowCount;
  }
  public void setCurrentWorkflowCount(Integer currentWorkflowCount) {
    this.currentWorkflowCount = currentWorkflowCount;
  }
  public Integer getCurrentConcurrentWorkflows() {
    return currentConcurrentWorkflows;
  }
  public void setCurrentConcurrentWorkflows(Integer currentConcurrentWorkflows) {
    this.currentConcurrentWorkflows = currentConcurrentWorkflows;
  }
  public Integer getCurrentWorkflowExecutionMonthly() {
    return currentWorkflowExecutionMonthly;
  }
  public void setCurrentWorkflowExecutionMonthly(Integer currentWorkflowExecutionMonthly) {
    this.currentWorkflowExecutionMonthly = currentWorkflowExecutionMonthly;
  }
  public Date getResetDate() {
    return resetDate;
  }
  public void setResetDate(Date resetDate) {
    this.resetDate = resetDate;
  }
  
}
