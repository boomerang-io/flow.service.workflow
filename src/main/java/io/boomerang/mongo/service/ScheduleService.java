package io.boomerang.mongo.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.mongo.model.WorkflowScheduleStatus;

public interface ScheduleService {

  void deleteSchedule(String id);

  WorkflowScheduleEntity getSchedule(String id);

  List<WorkflowScheduleEntity> getSchedulesForWorkflow(String workflowId);

  WorkflowScheduleEntity saveSchedule(WorkflowScheduleEntity entity);

  List<WorkflowScheduleEntity> getSchedulesForWorkflowWithStatus(String workflowId,
      WorkflowScheduleStatus status);

  List<WorkflowScheduleEntity> getSchedules(List<String> ids);

  List<WorkflowScheduleEntity> getAllSchedulesNotDeleted(List<String> workflowIds,
      Optional<List<String>> statuses, Optional<List<String>> types);

  List<WorkflowScheduleEntity> getSchedulesForWorkflowNotDeleted(String workflowId);

  List<WorkflowScheduleEntity> getSchedulesNotDeleted(List<String> ids);


}
