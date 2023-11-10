package io.boomerang.data.model;

import java.util.Date;
import org.springframework.beans.BeanUtils;

public class CurrentQuotas extends Quotas {
  
  private Integer currentWorkflowCount;
  private Integer currentRuns;
  private Integer currentConcurrentRuns;
  private Integer currentRunTotalDuration;
  private Integer currentRunMedianDuration;
  // TODO: future - can't currently calculate this easily
//  private Integer currentTotalWorkflowStorage;
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
  public Date getMonthlyResetDate() {
    return monthlyResetDate;
  }
  public void setMonthlyResetDate(Date monthlyResetDate) {
    this.monthlyResetDate = monthlyResetDate;
  }
  public Integer getCurrentRunMedianDuration() {
    return currentRunMedianDuration;
  }
  public void setCurrentRunMedianDuration(Integer currentRunMedianDuration) {
    this.currentRunMedianDuration = currentRunMedianDuration;
  }
}
