package io.boomerang.model.ref;

import java.util.Date;
import io.boomerang.model.enums.ref.RunStatus;

public class WorkflowRunSummary {

  private Date creationDate;
  private long duration = 0L;
  private RunStatus status = RunStatus.notstarted;
  private String workflowRef;
  private String workflowName;

  @Override
  public String toString() {
    return "WorkflowRunSummary [creationDate=" + creationDate + ", duration=" + duration + ", status=" + status + ", workflowRef="
        + workflowRef + "]";
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public RunStatus getStatus() {
    return status;
  }

  public void setStatus(RunStatus status) {
    this.status = status;
  }

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
}
