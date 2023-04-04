package io.boomerang.v4.data.model;

import java.util.Date;

public class CurrentQuotas extends Quotas {
  
  private Integer currentWorkflowCount;
  private Integer currentConcurrentWorkflows;
  private Integer currentWorkflowExecutionMonthly;
  private Integer currentWorkflowsPersistentStorage;
  private Date monthlyResetDate;
  
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
}
