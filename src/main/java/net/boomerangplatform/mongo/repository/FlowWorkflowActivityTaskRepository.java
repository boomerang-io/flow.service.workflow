package net.boomerangplatform.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import net.boomerangplatform.mongo.entity.FlowTaskExecutionEntity;

public interface FlowWorkflowActivityTaskRepository
    extends MongoRepository<FlowTaskExecutionEntity, String> {

  List<FlowTaskExecutionEntity> findByactivityId(String activityId);

  FlowTaskExecutionEntity findByActivityIdAndTaskId(String activityId, String taskId);

  FlowTaskExecutionEntity findByActivityIdAndTaskName(String activityId, String taskName);
}
