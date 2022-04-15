package io.boomerang.service;

import io.boomerang.mongo.entity.ActivityEntity;

public interface EventingService {

  void publishWorkflowActivityStatusUpdateCE(ActivityEntity activityEntity);
}
