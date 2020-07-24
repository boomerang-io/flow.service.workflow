package net.boomerangplatform.iam.service;

import net.boomerangplatform.iam.model.IAMStatus;

public interface IAMClient {

  public void publishEvent(String executionId, String messageId, String activityName,
      IAMStatus status);

}
