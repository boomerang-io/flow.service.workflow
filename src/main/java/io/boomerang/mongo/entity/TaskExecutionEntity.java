package io.boomerang.mongo.entity;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows_activity_task')}")
public class TaskExecutionEntity {

  private String activityId;

  private long duration;

  private TaskStatus flowTaskStatus;

  @Id
  private String id;

  private String nodeId;

  private long order;
  private Date startTime;
  private String taskId;
  private String taskName;
  private String workflowId;
  private String templateId;
  private Integer templateRevision;
  
  private TaskType taskType;
  
  private boolean preApproved;
  private String switchValue;
  
  private ApprovalEntity approval;
  
  private String runWorkflowActivityId;
  private String runWorkflowId;
  private TaskStatus runWorkflowActivityStatus;
  
  private ErrorResponse error;
  private Map<String, String> outputs;
  
  private List<KeyValuePair> outputProperties;
  
  public TaskType getTaskType() {
    return taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
  }

  public String getActivityId() {
    return activityId;
  }

  public long getDuration() {
    return duration;
  }

  public TaskStatus getFlowTaskStatus() {
    return flowTaskStatus;
  }

  public String getId() {
    return id;
  }

  public String getNodeId() {
    return nodeId;
  }

  public long getOrder() {
    return order;
  }

  public Date getStartTime() {
    return startTime;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getTaskName() {
    return taskName;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public void setFlowTaskStatus(TaskStatus flowTaskStatus) {
    this.flowTaskStatus = flowTaskStatus;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setNodeId(String nodeId) {
    this.nodeId = nodeId;
  }

  public void setOrder(long order) {
    this.order = order;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public Map<String, String> getOutputs() {
    return outputs;
  }

  public void setOutputs(Map<String, String> outputs) {
    this.outputs = outputs;
  }

  public ApprovalEntity getApproval() {
    return approval;
  }

  public void setApproval(ApprovalEntity approval) {
    this.approval = approval;
  }

  public boolean isPreApproved() {
    return preApproved;
  }

  public void setPreApproved(boolean preApproved) {
    this.preApproved = preApproved;
  }

  public String getSwitchValue() {
    return switchValue;
  }

  public void setSwitchValue(String switchValue) {
    this.switchValue = switchValue;
  }

  public String getRunWorkflowActivityId() {
    return runWorkflowActivityId;
  }

  public void setRunWorkflowActivityId(String runWorkflowActivityId) {
    this.runWorkflowActivityId = runWorkflowActivityId;
  }

  public String getRunWorkflowId() {
    return runWorkflowId;
  }

  public void setRunWorkflowId(String runWorkflowId) {
    this.runWorkflowId = runWorkflowId;
  }

  public TaskStatus getRunWorkflowActivityStatus() {
    return runWorkflowActivityStatus;
  }

  public void setRunWorkflowActivityStatus(TaskStatus runWorkflowActivityStatus) {
    this.runWorkflowActivityStatus = runWorkflowActivityStatus;
  }

  public ErrorResponse getError() {
    return error;
  }

  public void setError(ErrorResponse error) {
    this.error = error;
  }

  public String getTemplateId() {
    return templateId;
  }

  public void setTemplateId(String templateId) {
    this.templateId = templateId;
  }

  public Integer getTemplateRevision() {
    return templateRevision;
  }

  public void setTemplateRevision(Integer templateRevision) {
    this.templateRevision = templateRevision;
  }

  public List<KeyValuePair> getOutputProperties() {
    return outputProperties;
  }

  public void setOutputProperties(List<KeyValuePair> outputProperties) {
    this.outputProperties = outputProperties;
  }
  
}
