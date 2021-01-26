package net.boomerangplatform.mongo.model.internal;

import java.util.HashMap;
import java.util.Map;
import net.boomerangplatform.mongo.model.TaskStatus;

public class InternalTaskResponse {

  private TaskStatus status;

  private String activityId;
  
  private Map<String, String> outputProperties = new HashMap<String, String>();

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public Map<String, String> getOutputProperties() {
    return outputProperties;
  }

  public void setOutputProperties(Map<String, String> outputProperties) {
    this.outputProperties = outputProperties;
  }

}
