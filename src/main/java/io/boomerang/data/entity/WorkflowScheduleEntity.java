package io.boomerang.data.entity;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.boomerang.model.enums.WorkflowScheduleStatus;
import io.boomerang.model.enums.WorkflowScheduleType;
import io.boomerang.model.ref.RunParam;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "#{@mongoConfiguration.fullCollectionName('workflow_schedules')}")
public class WorkflowScheduleEntity {

  private String id;
  private String workflowRef;
  private String name;
  private String description;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private Date creationDate = new Date();
  private WorkflowScheduleType type = WorkflowScheduleType.cron;
  private WorkflowScheduleStatus status = WorkflowScheduleStatus.active;
  private Map<String, String> labels = new HashMap<>();
  private String cronSchedule;
  private Date dateSchedule;
  private String timezone;
  private List<RunParam> params = new LinkedList<>();
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getWorkflowRef() {
    return workflowRef;
  }
  public void setWorkflowRef(String workflowId) {
    this.workflowRef = workflowId;
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
  public Map<String, String> getLabels() {
    return labels;
  }
  public void setLabels(Map<String, String> labels) {
    this.labels = labels;
  }
  public String getCronSchedule() {
    return cronSchedule;
  }
  public void setCronSchedule(String cronSchedule) {
    this.cronSchedule = cronSchedule;
  }
  public Date getDateSchedule() {
    return dateSchedule;
  }
  public void setDateSchedule(Date dateSchedule) {
    this.dateSchedule = dateSchedule;
  }
  public String getTimezone() {
    return timezone;
  }
  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }
  public List<RunParam> getParams() {
    return params;
  }
  public void setParams(List<RunParam> params) {
    this.params = params;
  }
}
