package io.boomerang.data.model;

public class Quotas {

  private Integer maxWorkflowCount;
  private Integer maxWorkflowRunMonthly;
  private Integer maxWorkflowStorage;
  private Integer maxWorkflowRunTime;
  private Integer maxConcurrentRuns;
  
  public Integer getMaxWorkflowCount() {
    return maxWorkflowCount;
  }
  public void setMaxWorkflowCount(Integer maxWorkflowCount) {
    this.maxWorkflowCount = maxWorkflowCount;
  }
  public Integer getMaxWorkflowRunMonthly() {
    return maxWorkflowRunMonthly;
  }
  public void setMaxWorkflowRunMonthly(Integer maxWorkflowRunMonthly) {
    this.maxWorkflowRunMonthly = maxWorkflowRunMonthly;
  }
  public Integer getMaxWorkflowStorage() {
    return maxWorkflowStorage;
  }
  public void setMaxWorkflowStorage(Integer maxWorkflowStorage) {
    this.maxWorkflowStorage = maxWorkflowStorage;
  }
  public Integer getMaxWorkflowRunTime() {
    return maxWorkflowRunTime;
  }
  public void setMaxWorkflowRunTime(Integer maxWorkflowRunTime) {
    this.maxWorkflowRunTime = maxWorkflowRunTime;
  }
  public Integer getMaxConcurrentRuns() {
    return maxConcurrentRuns;
  }
  public void setMaxConcurrentRuns(Integer maxConcurrentRuns) {
    this.maxConcurrentRuns = maxConcurrentRuns;
  }
}
