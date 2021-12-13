package io.boomerang.mongo.entity;

import java.util.Date;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.mongo.model.WorkflowScheduleStatus;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflows_schedules')}")
public class WorkflowScheduleEntity {

  private String id;
  private String workflowId;
  private Date creationDate;
  private WorkflowScheduleStatus status = WorkflowScheduleStatus.active;
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
  public Date getCreationDate() {
    return creationDate;
  }
  public void setCreationDate(Date creationDate) {
    this.creationDate = creationDate;
  }  
  public WorkflowScheduleStatus getStatus() {
    return status;
  }
  public void setStatus(WorkflowScheduleStatus status) {
    this.status = status;
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
