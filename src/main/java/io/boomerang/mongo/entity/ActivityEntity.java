package io.boomerang.mongo.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.TaskStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows_activity')}")
public class ActivityEntity   {

  private List<KeyValuePair> labels;
  private Date creationDate;

  private Long duration;

  @Id
  private String id;

  private String initiatedByUserId;

  private String initiatedByUserName;

  private TaskStatus status;
  
  private TaskStatus statusOverride;


  public List<TaskWorkspace> getTaskWorkspaces() {
    return taskWorkspaces;
  }

  public void setTaskWorkspaces(List<TaskWorkspace> taskWorkspaces) {
    this.taskWorkspaces = taskWorkspaces;
  }

  private String statusMessage;

  private String workflowId;

  private String workflowRevisionid;

  private String trigger;

  private List<KeyValuePair> properties;

  private List<KeyValuePair> outputProperties;
  
  private boolean isAwaitingApproval;
  
  private String teamId;
  
  private String switchValue;
  
  private ErrorResponse error;
  
  @JsonProperty("workspaces")
  private List<TaskWorkspace> taskWorkspaces;
  
  public Date getCreationDate() {
    return creationDate;
  }

  public String getId() {
    return id;
  }

  public String getInitiatedByUserId() {
    return initiatedByUserId;
  }

  public String getInitiatedByUserName() {
    return initiatedByUserName;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public String getWorkflowRevisionid() {
    return workflowRevisionid;
  }

  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setInitiatedByUserId(String initiatedByUserId) {
    this.initiatedByUserId = initiatedByUserId;
  }

  public void setInitiatedByUserName(String initiatedByUserName) {
    this.initiatedByUserName = initiatedByUserName;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public void setWorkflowRevisionid(String workflowRevisionid) {
    this.workflowRevisionid = workflowRevisionid;
  }

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public List<KeyValuePair> getProperties() {
    return properties;
  }

  public void setProperties(List<KeyValuePair> properties) {
    this.properties = properties;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public Long getDuration() {
    return duration;
  }

  public void setDuration(Long duration) {
    this.duration = duration;
  }

  public List<KeyValuePair> getOutputProperties() {
    return outputProperties;
  }

  public void setOutputProperties(List<KeyValuePair> outputProperties) {
    this.outputProperties = outputProperties;
  }

  public boolean isAwaitingApproval() {
    return isAwaitingApproval;
  }

  public void setAwaitingApproval(boolean isAwaitingApproval) {
    this.isAwaitingApproval = isAwaitingApproval;
  }

  public String getTeamId() {
    return teamId;
  }

  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }

  public String getSwitchValue() {
    return switchValue;
  }

  public void setSwitchValue(String switchValue) {
    this.switchValue = switchValue;
  }

  public ErrorResponse getError() {
    return error;
  }

  public void setError(ErrorResponse error) {
    this.error = error;
  }

  public TaskStatus getStatusOverride() {
    return statusOverride;
  }

  public void setStatusOverride(TaskStatus statusOverride) {
    this.statusOverride = statusOverride;
  }

  public List<KeyValuePair> getLabels() {
    return labels;
  }

  public void setLabels(List<KeyValuePair> labels) {
    this.labels = labels;
  }
  
}
