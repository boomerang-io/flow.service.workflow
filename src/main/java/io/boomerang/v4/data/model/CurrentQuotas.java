package io.boomerang.v4.data.model;

import java.util.Date;
import org.springframework.beans.BeanUtils;

public class CurrentQuotas extends Quotas {
  
  private Integer currentWorkflowCount;
  private Integer currentRuns;
  private Integer currentConcurrentRuns;
  private Integer currentRunTotalDuration;
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
  public Integer getCurrentRuns() {
    return currentRuns;
  }
  public void setCurrentRuns(Integer currentRuns) {
    this.currentRuns = currentRuns;
  }
  public Integer getCurrentConcurrentWorkflows() {
    return currentConcurrentRuns;
  }
  public void setCurrentConcurrentWorkflows(Integer currentConcurrentWorkflows) {
    this.currentConcurrentRuns = currentConcurrentWorkflows;
  }
  public Integer getCurrentRunTotalDuration() {
    return currentRunTotalDuration;
  }
  public void setCurrentRunTotalDuration(Integer currentRunDuration) {
    this.currentRunTotalDuration = currentRunDuration;
  }
  public Integer getCurrentPersistentStorage() {
    return currentPersistentStorage;
  }
  public void setCurrentPersistentStorage(Integer currentPersistentStorage) {
    this.currentPersistentStorage = currentPersistentStorage;
  }
  public Date getMonthlyResetDate() {
    return monthlyResetDate;
  }
  public void setMonthlyResetDate(Date monthlyResetDate) {
    this.monthlyResetDate = monthlyResetDate;
  }
}
