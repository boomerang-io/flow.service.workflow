package net.boomerangplatform.model;

import java.util.List;
import org.springframework.beans.BeanUtils;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;

public class FlowActivity extends ActivityEntity {

  private String description;

  private String icon;

  private String shortDescription;

  private List<TaskExecutionEntity> steps;

  private String userName;

  private String workflowName;

  private String teamName;
  
  public FlowActivity() {
    
  }

  public FlowActivity(ActivityEntity entity) {
    BeanUtils.copyProperties(entity, this);
  }

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public List<TaskExecutionEntity> getSteps() {
    return steps;
  }

  public String getUserName() {
    return userName;
  }

  public String getWorkflowName() {
    return workflowName;
  }

  public void setSteps(List<TaskExecutionEntity> steps) {
    this.steps = steps;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
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
}
