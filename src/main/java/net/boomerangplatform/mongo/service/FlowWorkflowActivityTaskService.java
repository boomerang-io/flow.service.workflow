package net.boomerangplatform.mongo.service;

import java.util.List;
import net.boomerangplatform.mongo.entity.FlowTaskExecutionEntity;

public interface FlowWorkflowActivityTaskService {

  FlowTaskExecutionEntity findByTaskNameAndActiityId(String taskName, String activityId);

  FlowTaskExecutionEntity findByTaskIdAndActiityId(String taskId, String activityId);

  List<FlowTaskExecutionEntity> findTaskActiivtyForActivity(String activityId);

  FlowTaskExecutionEntity save(FlowTaskExecutionEntity entity);
}
