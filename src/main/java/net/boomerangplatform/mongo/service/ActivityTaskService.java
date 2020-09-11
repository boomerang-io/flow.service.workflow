package net.boomerangplatform.mongo.service;

import java.util.List;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;

public interface ActivityTaskService {

  
  TaskExecutionEntity findById(String id);
  
  TaskExecutionEntity findByTaskNameAndActiityId(String taskName, String activityId);

  TaskExecutionEntity findByTaskIdAndActiityId(String taskId, String activityId);

  List<TaskExecutionEntity> findTaskActiivtyForActivity(String activityId);

  TaskExecutionEntity save(TaskExecutionEntity entity);
}
