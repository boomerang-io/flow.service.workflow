package io.boomerang.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import io.boomerang.mongo.entity.TaskExecutionEntity;

public interface FlowWorkflowActivityTaskRepository
    extends MongoRepository<TaskExecutionEntity, String> {

  List<TaskExecutionEntity> findByactivityId(String activityId);

  TaskExecutionEntity findByActivityIdAndTaskId(String activityId, String taskId);

  TaskExecutionEntity findByActivityIdAndTaskName(String activityId, String taskName);
}
