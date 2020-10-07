package net.boomerangplatform.service.refactor;

import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;

public interface TaskService {

  void createTask(InternalTaskRequest request);

  void endTask(InternalTaskResponse request);

  String getTaskActivityForTopic(String activityId, String topic);
}
