package io.boomerang.model.ref;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.model.enums.ref.RunPhase;
import io.boomerang.model.enums.ref.RunStatus;

@JsonPropertyOrder({"id", "creationDate", "status", "phase", "startTime", "duration", "statusMessage", "error", "timeout", "retries", "workflowRef", "workflowRevisionRef", "labels", "annotations", "params", "tasks" })
public class WorkflowRun {
  
  @Id
  private String id;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private Date creationDate;
  private Date startTime;
  private long duration = 0;
  private Long timeout;
  private Long retries;
  private Boolean debug;
  private RunStatus status = RunStatus.notstarted;
  private RunPhase phase = RunPhase.pending;
  private String statusMessage;
  private boolean isAwaitingApproval;
  private String workflowRef;
  private String workflowName;
  private Integer workflowVersion;
  private String workflowRevisionRef;
  private String trigger;
  private String initiatedByRef;
  private List<RunParam> params = new LinkedList<>();
  private List<RunResult> results = new LinkedList<>();
  private List<WorkflowWorkspace> workspaces = new LinkedList<>();
  private List<TaskRun> tasks;

  @Override
  public String toString() {
    return "WorkflowRun [id=" + id + ", creationDate=" + creationDate + ", startTime=" + startTime
        + ", duration=" + duration + ", timeout=" + timeout + ", retries=" + retries + ", debug="
        + debug + ", status=" + status + ", phase=" + phase + ", statusMessage=" + statusMessage
        + ", isAwaitingApproval=" + isAwaitingApproval + ", workflowRef="
        + workflowRef + ", workflowName=" + workflowName + ", workflowVersion=" + workflowVersion
        + ", workflowRevisionRef=" + workflowRevisionRef + ", trigger=" + trigger + ", params="
        + params + ", results=" + results + ", workspaces=" + workspaces + "]";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
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

  public Long getTimeout() {
    return timeout;
  }

  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }

  public Long getRetries() {
    return retries;
  }

  public void setRetries(Long retries) {
    this.retries = retries;
  }

  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public RunStatus getStatus() {
    return status;
  }

  public void setStatus(RunStatus status) {
    this.status = status;
  }

  public RunPhase getPhase() {
    return phase;
  }

  public void setPhase(RunPhase phase) {
    this.phase = phase;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public boolean isAwaitingApproval() {
    return isAwaitingApproval;
  }

  public void setAwaitingApproval(boolean isAwaitingApproval) {
    this.isAwaitingApproval = isAwaitingApproval;
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

  public Integer getWorkflowVersion() {
    return workflowVersion;
  }

  public void setWorkflowVersion(Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
  }

  public String getWorkflowRevisionRef() {
    return workflowRevisionRef;
  }

  public void setWorkflowRevisionRef(String workflowRevisionRef) {
    this.workflowRevisionRef = workflowRevisionRef;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public String getInitiatedByRef() {
    return initiatedByRef;
  }

  public void setInitiatedByRef(String initiatedByRef) {
    this.initiatedByRef = initiatedByRef;
  }

  public List<RunParam> getParams() {
    return params;
  }

  public void setParams(List<RunParam> params) {
    this.params = params;
  }

  public List<RunResult> getResults() {
    return results;
  }

  public void setResults(List<RunResult> results) {
    this.results = results;
  }

  public List<WorkflowWorkspace> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<WorkflowWorkspace> workspaces) {
    this.workspaces = workspaces;
  }

  public List<TaskRun> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskRun> tasks) {
    this.tasks = tasks;
  }
}
