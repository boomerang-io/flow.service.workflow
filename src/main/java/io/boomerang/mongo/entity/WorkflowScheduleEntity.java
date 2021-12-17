package io.boomerang.mongo.entity;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.WorkflowScheduleStatus;
import io.boomerang.mongo.model.WorkflowScheduleType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows_schedules')}")
public class WorkflowScheduleEntity {

  private String id;
  private String workflowId;
  private String name;
  private String description;
  private Date creationDate;
  private WorkflowScheduleType type = WorkflowScheduleType.cron;
  private WorkflowScheduleStatus status = WorkflowScheduleStatus.active;
  private List<KeyValuePair> labels;
  private String cronSchedule;
  private String dateSchedule;
  private String timezone;
  private List<KeyValuePair> parameters = new LinkedList<>();
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getWorkflowId() {
    return workflowId;
  }
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }  
  public WorkflowScheduleType getType() {
    return type;
  }
  public void setType(WorkflowScheduleType type) {
    this.type = type;
  }
  public WorkflowScheduleStatus getStatus() {
    return status;
  }
  public void setStatus(WorkflowScheduleStatus status) {
    this.status = status;
  }
  public List<KeyValuePair> getLabels() {
    return labels;
  }
  public void setLabels(List<KeyValuePair> labels) {
    this.labels = labels;
  }
  public String getCronSchedule() {
    return cronSchedule;
  }
  public void setCronSchedule(String cronSchedule) {
    this.cronSchedule = cronSchedule;
  }
  public String getDateSchedule() {
    return dateSchedule;
  }
  public void setDateSchedule(String dateSchedule) {
    this.dateSchedule = dateSchedule;
  }
  public String getTimezone() {
    return timezone;
  }
  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }
  public List<KeyValuePair> getParameters() {
    return parameters;
  }
  public void setParameters(List<KeyValuePair> parameters) {
    this.parameters = parameters;
  }
}
