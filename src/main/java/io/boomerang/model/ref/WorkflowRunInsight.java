package io.boomerang.model.ref;

public class WorkflowRunInsight {

  private Long totalRuns;
  private Long concurrentRuns;
  private Long totalDuration;
  private Long medianDuration;

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

  public Long getMedianDuration() {
    return medianDuration;
  }

  public void setMedianDuration(Long medianDuration) {
    this.medianDuration = medianDuration;
  }
}
