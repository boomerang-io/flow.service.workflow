package net.boomerangplatform.mongo.model.internal;

import net.boomerangplatform.mongo.model.TaskStatus;

public class InternalTaskResponse {
	
	private TaskStatus status;
	
	private String activityId;

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

}
