package io.boomerang.model.ref;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.WorkflowCanvas;
import io.boomerang.model.enums.ref.WorkflowStatus;

/*
 * Workflow Model joining Workflow Entity and Workflow Revision Entity
 * 
 * A number of the Workflow Revision elements are put under metadata
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({"id", "name", "status", "version", "creationDate", "timeout", "retries", "description", "labels", "annotations", "params", "tasks" })
public class Workflow {
  
  private String id;
  private String name;
  private WorkflowStatus status = WorkflowStatus.active;
  private Integer version = 1;
  private Date creationDate = new Date();
  private ChangeLog changelog;
  private String icon;
  private String description;
  private String markdown;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private Long timeout;
  private Long retries;
  private boolean upgradesAvailable = false;
  private WorkflowTrigger triggers = new WorkflowTrigger();
  private List<Task> tasks = new LinkedList<>();
  private List<ParamSpec> params = new LinkedList<>();
  private List<WorkflowWorkspace> workspaces = new LinkedList<>();  
  private List<AbstractParam> config = new LinkedList<>();;
  private Map<String, Object> unknownFields = new HashMap<>();
  
  public Workflow() {
  }
  
  /*
  * Creates a Workflow from WorkflowCanvas
  * 
  * Does not copy / convert the stored Tasks onto the Workflow. If you want the Tasks you need to run
  * workflow.setTasks(TaskMapper.revisionTasksToListOfTasks(wfRevisionEntity.getTasks()));
  */
  public Workflow(WorkflowCanvas wfCanvas) {
    BeanUtils.copyProperties(wfCanvas, this);
  }

  @Override
  public String toString() {
    return "Workflow [id=" + id + ", name=" + name + ", status=" + status + ", version=" + version
        + ", creationDate=" + creationDate + ", changelog=" + changelog + ", icon=" + icon
        + ", description=" + description + ", markdown=" + markdown + ", labels=" + labels
        + ", annotations=" + annotations + ", timeout=" + timeout + ", retries=" + retries
        + ", upgradesAvailable=" + upgradesAvailable + ", triggers=" + triggers + ", tasks=" + tasks
        + ", params=" + params + ", workspaces=" + workspaces + ", config=" + config
        + ", unknownFields=" + unknownFields + "]";
  }

  @JsonAnyGetter
  @JsonPropertyOrder(alphabetic = true)
  public Map<String, Object> otherFields() {
    return unknownFields;
  }

  @JsonAnySetter
  public void setOtherField(String name, Object value) {
    unknownFields.put(name, value);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowStatus getStatus() {
    return status;
  }

  public void setStatus(WorkflowStatus status) {
    this.status = status;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public ChangeLog getChangelog() {
    return changelog;
  }

  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
  }

  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
  
  public String getMarkdown() {
    return markdown;
  }

  public void setMarkdown(String markdown) {
    this.markdown = markdown;
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

  public List<Task> getTasks() {
    return tasks;
  }

  public void setTasks(List<Task> tasks) {
    this.tasks = tasks;
  }

  public List<ParamSpec> getParams() {
    return params;
  }

  public void setParams(List<ParamSpec> params) {
    this.params = params;
  }

  public List<WorkflowWorkspace> getWorkspaces() {
    return workspaces;
  }

  public void setWorkspaces(List<WorkflowWorkspace> workspaces) {
    this.workspaces = workspaces;
  }

  public List<AbstractParam> getConfig() {
    return config;
  }

  public void setConfig(List<AbstractParam> config) {
    this.config = config;
  }

  public WorkflowTrigger getTriggers() {
    return triggers;
  }

  public void setTriggers(WorkflowTrigger triggers) {
    this.triggers = triggers;
  }

  public boolean isUpgradesAvailable() {
    return upgradesAvailable;
  }

  public void setUpgradesAvailable(boolean upgradesAvailable) {
    this.upgradesAvailable = upgradesAvailable;
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
}
