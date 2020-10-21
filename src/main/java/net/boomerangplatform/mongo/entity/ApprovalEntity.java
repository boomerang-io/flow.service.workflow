package net.boomerangplatform.mongo.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import net.boomerangplatform.model.ApprovalStatus;
import net.boomerangplatform.mongo.model.Audit;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "flow_workflows_activity_approval")
public class ApprovalEntity {
  
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  public String getActivityId() {
    return activityId;
  }
  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }
  public String getTaskActivityId() {
    return taskActivityId;
  }
  public void setTaskActivityId(String taskActivityId) {
    this.taskActivityId = taskActivityId;
  }
  public Audit getAudit() {
    return audit;
  }
  public void setAudit(Audit audit) {
    this.audit = audit;
  }
  @Id
  private String id;
  
  public String getWorkflowId() {
    return workflowId;
  }
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
  public String getTeamId() {
    return teamId;
  }
  public void setTeamId(String teamId) {
    this.teamId = teamId;
  }
  public ApprovalStatus getStatus() {
    return status;
  }
  public void setStatus(ApprovalStatus status) {
    this.status = status;
  }
 
  private String activityId;
  
  private String taskActivityId;
  private String workflowId;
  private String teamId;
  private Audit audit;
  private ApprovalStatus status;

 
}
