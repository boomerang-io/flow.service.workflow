package net.boomerangplatform.mongo.model;

public class Quotas {

  private Integer maxWorkflowCount;
  private Integer maxWorkflowExecutionMonthly;
  private Integer maxWorkflowStorage;
  private Integer maxWorkflowExecutionTime;
  private Integer maxConcurrentWorkflows;
  
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
