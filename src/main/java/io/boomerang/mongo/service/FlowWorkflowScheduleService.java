package io.boomerang.mongo.service;

import java.util.List;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;

public interface FlowWorkflowScheduleService {

  void deleteSchedule(String id);

  WorkflowScheduleEntity getSchedule(String id);

  List<WorkflowScheduleEntity> getSchedulesForWorkflow(String workflowId);

  WorkflowScheduleEntity saveSchedule(WorkflowScheduleEntity entity);


}
