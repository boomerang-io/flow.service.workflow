package io.boomerang.service.crud;

import java.util.Date;
import java.util.List;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.WorkflowScheduleCalendar;

public interface WorkflowScheduleService {

  void enableSchedule(String scheduleId) throws SchedulerException;

  void disableSchedule(String scheduleId) throws SchedulerException;

  void deleteAllSchedules(String workflowId) throws SchedulerException;

  WorkflowSchedule createSchedule(WorkflowSchedule schedule);

  List<WorkflowSchedule> getSchedules(String workflowId);

  WorkflowSchedule getSchedule(String scheduleId);

  ResponseEntity<?> deleteSchedule(String scheduleId);

  WorkflowSchedule updateSchedule(String scheduleId,
      WorkflowSchedule patchSchedule);

  CronValidationResponse validateCron(String cronString);

  List<WorkflowScheduleCalendar> getSchedulesForDates(String workflowId, Date fromDate, Date toDate);

  List<Date> getScheduleForDates(String scheduleId, Date fromDate, Date toDate);

  void disableAllSchedules(String workflowId);

  void enableAllSchedules(String workflowId);

  void enableAllTriggerSchedules(String workflowId);

  void disableTriggerSchedule(String scheduleId) throws SchedulerException;

  void disableAllTriggerSchedules(String workflowId);
}
