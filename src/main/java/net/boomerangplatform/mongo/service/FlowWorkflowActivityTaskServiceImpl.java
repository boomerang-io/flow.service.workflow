package net.boomerangplatform.mongo.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.mongo.entity.FlowTaskExecutionEntity;
import net.boomerangplatform.mongo.repository.FlowWorkflowActivityTaskRepository;

@Service
public class FlowWorkflowActivityTaskServiceImpl implements FlowWorkflowActivityTaskService {

  @Autowired
  private FlowWorkflowActivityTaskRepository repoisotry;

  @Override
  public FlowTaskExecutionEntity findByTaskIdAndActiityId(String taskId, String activityId) {
    return repoisotry.findByActivityIdAndTaskId(activityId, taskId);

  }

  @Override
  public List<FlowTaskExecutionEntity> findTaskActiivtyForActivity(String activityId) {
    return repoisotry.findByactivityId(activityId);
  }

  @Override
  public FlowTaskExecutionEntity save(FlowTaskExecutionEntity entity) {
    return repoisotry.save(entity);
  }

  @Override
  public FlowTaskExecutionEntity findByTaskNameAndActiityId(String taskName, String activityId) {
    return repoisotry.findByActivityIdAndTaskName(activityId, taskName);
  }

}
