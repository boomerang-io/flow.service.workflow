package io.boomerang.service.crud;

import org.quartz.SchedulerException;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;

public interface WorkflowScheduleService {

  void createSchedule(WorkflowScheduleEntity schedule);

  void updateSchedule(WorkflowScheduleEntity schedule);

  void enableSchedule(String scheduleId) throws SchedulerException;

  void disableSchedule(String scheduleId) throws SchedulerException;

  void deleteAllSchedules(String workflowId) throws SchedulerException;
}
