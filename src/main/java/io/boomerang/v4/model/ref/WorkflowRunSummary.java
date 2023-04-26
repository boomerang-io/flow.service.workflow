package io.boomerang.v4.model.ref;

import java.util.Date;
import org.springframework.beans.BeanUtils;
import io.boomerang.data.entity.WorkflowRunEntity;
import io.boomerang.model.enums.RunStatus;

public class WorkflowRunSummary {

  private String id;
  private Date creationDate;
  private Date startTime;
  private long duration;
  private RunStatus status;
  private String workflowRef;
  
  public WorkflowRunSummary() {
    
  }

  public WorkflowRunSummary(WorkflowRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  @Override
  public String toString() {
    return "WorkflowRunSummary [id=" + id + ", creationDate=" + creationDate + ", startTime="
        + startTime + ", duration=" + duration + ", status=" + status + ", workflowRef="
        + workflowRef + "]";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
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
}
