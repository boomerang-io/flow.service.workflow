package io.boomerang.mongo.service;

import java.util.List;
import io.boomerang.mongo.entity.TaskExecutionEntity;

public interface ActivityTaskService {

  
  TaskExecutionEntity findById(String id);
  
  TaskExecutionEntity findByTaskNameAndActivityId(String taskName, String activityId);

  TaskExecutionEntity findByTaskIdAndActivityId(String taskId, String activityId);

  List<TaskExecutionEntity> findTaskActiivtyForActivity(String activityId);

  TaskExecutionEntity save(TaskExecutionEntity entity);
}
