package net.boomerangplatform.service.refactor;

import java.util.List;
import java.util.Map;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;

public interface TaskService {

  void createTask(InternalTaskRequest request);

  void endTask(InternalTaskResponse request);

  List<String> updateTaskActivityForTopic(String activityId, String topic);
  
  void submitActivity(String taskActivityId, String taskStatus, Map<String, String> outputProperties);
  
}
