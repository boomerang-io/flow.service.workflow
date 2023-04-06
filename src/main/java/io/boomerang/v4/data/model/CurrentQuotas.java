package io.boomerang.v4.data.model;

import java.util.Date;
import org.springframework.beans.BeanUtils;

public class CurrentQuotas extends Quotas {
  
  private Integer currentWorkflowCount;
  private Integer currentConcurrentRuns;
  private Integer currentRunDuration;
  private Integer currentPersistentStorage;
  private Date monthlyResetDate;
  
  public CurrentQuotas() {
  }
  public CurrentQuotas(Quotas quotas) {
    BeanUtils.copyProperties(quotas, this);
  }
  
  public Integer getCurrentWorkflowCount() {
    return currentWorkflowCount;
  }
  public void setCurrentWorkflowCount(Integer currentWorkflowCount) {
    this.currentWorkflowCount = currentWorkflowCount;
  }
  public Integer getCurrentConcurrentWorkflows() {
    return currentConcurrentRuns;
  }
  public void setCurrentConcurrentWorkflows(Integer currentConcurrentWorkflows) {
    this.currentConcurrentRuns = currentConcurrentWorkflows;
  }
  public Integer getCurrentWorkflowExecutionMonthly() {
    return currentRunDuration;
  }
  public void setCurrentWorkflowExecutionMonthly(Integer currentWorkflowExecutionMonthly) {
    this.currentRunDuration = currentWorkflowExecutionMonthly;
  }
  public Integer getCurrentWorkflowsPersistentStorage() {
    return currentPersistentStorage;
  }
  public void setCurrentWorkflowsPersistentStorage(Integer currentWorkflowsPersistentStorage) {
    this.currentPersistentStorage = currentWorkflowsPersistentStorage;
  }
  public Date getMonthlyResetDate() {
    return monthlyResetDate;
  }
  public void setMonthlyResetDate(Date monthlyResetDate) {
    this.monthlyResetDate = monthlyResetDate;
  }
}
