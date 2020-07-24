package net.boomerangplatform.model;

import java.util.List;

public class InsightsSummary {

  private Integer totalActivitiesExecuted;
  private Long medianExecutionTime;
  private List<Execution> executions;

  public Integer getTotalActivitiesExecuted() {
    return totalActivitiesExecuted;
  }

  public void setTotalActivitiesExecuted(Integer totalActivitiesExecuted) {
    this.totalActivitiesExecuted = totalActivitiesExecuted;
  }

  public Long getMedianExecutionTime() {
    return medianExecutionTime;
  }

  public void setMedianExecutionTime(Long medianExecutionTime) {
    this.medianExecutionTime = medianExecutionTime;
  }

  public List<Execution> getExecutions() {
    return executions;
  }

  public void setExecutions(List<Execution> executions) {
    this.executions = executions;
  }

}
