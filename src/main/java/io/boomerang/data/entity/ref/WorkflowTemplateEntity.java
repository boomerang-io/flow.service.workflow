package io.boomerang.data.entity.ref;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.data.model.ref.WorkflowTask;
import io.boomerang.model.AbstractParam;
import io.boomerang.model.ref.ChangeLog;
import io.boomerang.model.ref.ParamSpec;
import io.boomerang.model.ref.WorkflowWorkspace;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_templates')}")
public class WorkflowTemplateEntity {
  
  @Id
  @JsonIgnore
  private String id;
  @Indexed
  private String name;
  private String displayName;
  private Date creationDate = new Date();
  @Indexed
  private Integer version;
  private String icon;
  private String description;
  private String markdown;
  private Map<String, String> labels = new HashMap<>();
  private Map<String, Object> annotations = new HashMap<>();
  private List<WorkflowTask> tasks = new LinkedList<>();
  private ChangeLog changelog;
  private List<ParamSpec> params;
  private List<WorkflowWorkspace> workspaces;
  private List<AbstractParam> config;
  private Long timeout;
  private Long retries;
  
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
  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }
  public Integer getVersion() {
    return version;
  }
  public void setVersion(Integer version) {
    this.version = version;
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
  public List<WorkflowTask> getTasks() {
    return tasks;
  }
  public void setTasks(List<WorkflowTask> tasks) {
    this.tasks = tasks;
  }
  public ChangeLog getChangelog() {
    return changelog;
  }
  public void setChangelog(ChangeLog changelog) {
    this.changelog = changelog;
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
