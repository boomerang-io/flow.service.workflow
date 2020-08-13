package net.boomerangplatform.model;

import java.util.Date;

public class WorkflowQuotas {
  
  private Integer currentWorkflowCount;
  private Integer currentConcurrentWorkflows;
  private Integer currentWorkflowExecutionMonthly;
  private Integer currentWorkflowsPersistentStorage;
  private Date monthlyResetDate;
  private Integer maxWorkflowCount;
  private Integer maxWorkflowExecutionMonthly;
  private Integer maxWorkflowStorage;
  private Integer maxWorkflowExecutionTime;
  private Integer maxConcurrentWorkflows;
  
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
  public Integer getCurrentWorkflowsPersistentStorage() {
    return currentWorkflowsPersistentStorage;
  }
  public void setCurrentWorkflowsPersistentStorage(Integer currentWorkflowsPersistentStorage) {
    this.currentWorkflowsPersistentStorage = currentWorkflowsPersistentStorage;
  }
  public Date getMonthlyResetDate() {
    return monthlyResetDate;
  }
  public void setMonthlyResetDate(Date monthlyResetDate) {
    this.monthlyResetDate = monthlyResetDate;
  }
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

}
