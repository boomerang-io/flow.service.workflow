package io.boomerang.service.crud;

import java.util.Date;
import java.util.List;
import java.util.Optional;
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

  List<WorkflowSchedule> getSchedules(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> statuses, Optional<List<String>> types,
      Optional<List<String>> scopes);

  WorkflowSchedule getSchedule(String scheduleId);

  ResponseEntity<?> deleteSchedule(String scheduleId);

  WorkflowSchedule updateSchedule(String scheduleId,
      WorkflowSchedule patchSchedule);

  CronValidationResponse validateCron(String cronString);

  List<Date> getCalendarForDates(String scheduleId, Date fromDate, Date toDate);

  void disableAllSchedules(String workflowId);

  void enableAllSchedules(String workflowId);

  void enableAllTriggerSchedules(String workflowId);

  void disableTriggerSchedule(String scheduleId) throws SchedulerException;

  void disableAllTriggerSchedules(String workflowId);

  List<WorkflowScheduleCalendar> getCalendarsForWorkflow(String workflowId, Date fromDate,
      Date toDate);

  List<WorkflowScheduleCalendar> getCalendarsForSchedules(List<String> scheduleIds, Date fromDate,
      Date toDate);

  List<WorkflowSchedule> getSchedulesForWorkflow(String workflowId);
}
