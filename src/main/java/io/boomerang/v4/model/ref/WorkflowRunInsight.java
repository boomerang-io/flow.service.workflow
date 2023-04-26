package io.boomerang.v4.model.ref;

import java.util.List;

public class WorkflowRunInsight {

  private Long totalRuns;
  private Long concurrentRuns;
  private Long totalDuration;
  private Long medianDuration;
  private List<WorkflowRunSummary> runs;

  public Long getTotalRuns() {
    return totalRuns;
  }

  public void setTotalRuns(Long totalRuns) {
    this.totalRuns = totalRuns;
  }

  public Long getConcurrentRuns() {
    return concurrentRuns;
  }

  public void setConcurrentRuns(Long concurrentRuns) {
    this.concurrentRuns = concurrentRuns;
  }

  public Long getTotalDuration() {
    return totalDuration;
  }

  public void setTotalDuration(Long totalDuration) {
    this.totalDuration = totalDuration;
  }

  public long getMedianDuration() {
    return medianDuration;
  }

  public void setMedianDuration(Long medianDuration) {
    this.medianDuration = medianDuration;
  }

  public List<WorkflowRunSummary> getRuns() {
    return runs;
  }

  public void setRuns(List<WorkflowRunSummary> runs) {
    this.runs = runs;
  }

}
