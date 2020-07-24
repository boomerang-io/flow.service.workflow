package net.boomerangplatform.mongo.entity;

import java.util.Date;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import net.boomerangplatform.mongo.model.CoreProperty;
import net.boomerangplatform.mongo.model.FlowTaskStatus;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@Document(collection = "flow_workflows_activity")
public class FlowWorkflowActivityEntity {

  private Date creationDate;

  private Long duration;

  @Id
  private String id;

  private String initiatedByUserId;

  private String initiatedByUserName;

  private FlowTaskStatus status;

  private String statusMessage;

  private String workflowId;

  private String workflowRevisionid;

  private FlowTriggerEnum trigger;

  private List<CoreProperty> properties;

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

  public FlowTaskStatus getStatus() {
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

  public void setStatus(FlowTaskStatus status) {
    this.status = status;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public void setWorkflowRevisionid(String workflowRevisionid) {
    this.workflowRevisionid = workflowRevisionid;
  }

  public FlowTriggerEnum getTrigger() {
    return trigger;
  }

  public void setTrigger(FlowTriggerEnum trigger) {
    this.trigger = trigger;
  }

  public List<CoreProperty> getProperties() {
    return properties;
  }

  public void setProperties(List<CoreProperty> properties) {
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

}
