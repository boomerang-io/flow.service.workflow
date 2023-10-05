package io.boomerang.data.entity.ref;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.boomerang.model.enums.ref.RunPhase;
import io.boomerang.model.enums.ref.RunStatus;
import io.boomerang.model.enums.ref.TaskType;
import io.boomerang.model.ref.ResultSpec;
import io.boomerang.model.ref.RunError;
import io.boomerang.model.ref.RunParam;
import io.boomerang.model.ref.RunResult;
import io.boomerang.model.ref.TaskDependency;
import io.boomerang.model.ref.TaskRunSpec;
import io.boomerang.model.ref.TaskWorkspace;

@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('task_runs')}")
public class TaskRunEntity {

  @Id
  private String id;
  
  private TaskType type;
  
  private String name;

  private Map<String, String> labels = new HashMap<>();

  private Map<String, Object> annotations = new HashMap<>();
  
  private Date creationDate;
  
  private Date startTime;

  private long duration;
  
  private Long timeout;
  
//  private Long retries;
  
  private List<RunParam> params = new LinkedList<>();

  private List<RunResult> results = new LinkedList<>();
  
  private List<TaskWorkspace> workspaces = new LinkedList<>();
  
  private TaskRunSpec spec;

  private RunStatus status;
  
  private RunPhase phase;

  private String statusMessage;

  @JsonIgnore
  private boolean preApproved;

  @JsonIgnore
  private String decisionValue;

  @JsonIgnore
  private List<TaskDependency> dependencies;

  private String templateRef;

  private Integer templateVersion;

  private String workflowRef;

  private String workflowRevisionRef;

  private String workflowRunRef;

  @Override
  public String toString() {
    return "TaskRunEntity [id=" + id + ", type=" + type + ", name=" + name + ", labels=" + labels
        + ", creationDate=" + creationDate + ", startTime=" + startTime + ", duration=" + duration
        + ", timeout=" + timeout + ", params=" + params + ", results=" + results + ", workspaces="
        + workspaces + ", status=" + status + ", phase=" + phase + ", templateRef=" + templateRef
        + ", templateVersion=" + templateVersion + ", workflowRef=" + workflowRef
        + ", workflowRevisionRef=" + workflowRevisionRef + ", workflowRunRef=" + workflowRunRef
        + "]";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public TaskType getType() {
    return type;
  }

  public void setType(TaskType type) {
    this.type = type;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, String> getLabels() {
    return labels;
  }

  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }

  public void putLabels(Map<String, String> labels) {
    this.labels.putAll(labels);
  }

  public Map<String, Object> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(Map<String, Object> annotations) {
    this.annotations = annotations;
  }

  public void putAnnotations(Map<String, Object> annotations) {
    this.annotations.putAll(annotations);
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

  public void addResult(RunResult result) {
    this.results.add(result);
  }

  public List<TaskWorkspace> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<TaskWorkspace> workspaces) {
    this.workspaces = workspaces;
  }

  public TaskRunSpec getSpec() {
    return spec;
  }

  public void setSpec(TaskRunSpec spec) {
    this.spec = spec;
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

  public boolean isPreApproved() {
    return preApproved;
  }

  public void setPreApproved(boolean preApproved) {
    this.preApproved = preApproved;
  }

  public String getDecisionValue() {
    return decisionValue;
  }

  public void setDecisionValue(String decisionValue) {
    this.decisionValue = decisionValue;
  }

  public List<TaskDependency> getDependencies() {
    return dependencies;
  }

  public void setDependencies(List<TaskDependency> dependencies) {
    this.dependencies = dependencies;
  }

  public String getTemplateRef() {
    return templateRef;
  }

  public void setTemplateRef(String templateRef) {
    this.templateRef = templateRef;
  }

  public Integer getTemplateVersion() {
    return templateVersion;
  }

  public void setTemplateVersion(Integer templateVersion) {
    this.templateVersion = templateVersion;
  }

  public String getWorkflowRunRef() {
    return workflowRunRef;
  }

  public void setWorkflowRunRef(String workflowRunRef) {
    this.workflowRunRef = workflowRunRef;
  }

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

  public String getWorkflowRevisionRef() {
    return workflowRevisionRef;
  }

  public void setWorkflowRevisionRef(String workflowRevisionRef) {
    this.workflowRevisionRef = workflowRevisionRef;
  }
}
