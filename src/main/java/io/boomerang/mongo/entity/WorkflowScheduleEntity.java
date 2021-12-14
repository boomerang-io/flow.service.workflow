package io.boomerang.mongo.entity;

import java.util.Date;
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
  private Date creationDate;
  private WorkflowScheduleType type = WorkflowScheduleType.cron;
  private WorkflowScheduleStatus status = WorkflowScheduleStatus.active;
  private List<KeyValuePair> labels;
  private String schedule;
  private String timezone;
  private Boolean advancedCron;
  
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
  public String getSchedule() {
    return schedule;
  }
  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }
  public String getTimezone() {
    return timezone;
  }
  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }
  public Boolean getAdvancedCron() {
    return advancedCron;
  }
  public void setAdvancedCron(Boolean advancedCron) {
    this.advancedCron = advancedCron;
  }
}
