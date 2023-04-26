package io.boomerang.v4.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.WorkflowScheduleCalendar;

public interface ScheduleService {

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

  void enableAllTriggerSchedules(String workflowId);

  void disableAllTriggerSchedules(String workflowId);

  void deleteAllSchedules(String workflowId) throws SchedulerException;

  List<WorkflowScheduleCalendar> getCalendarsForWorkflow(String workflowId, Date fromDate,
      Date toDate);

  List<WorkflowScheduleCalendar> getCalendarsForSchedules(List<String> scheduleIds, Date fromDate,
      Date toDate);

  List<WorkflowSchedule> getSchedulesForWorkflow(String workflowId);

  ResponseEntity<?> completeSchedule(String scheduleId);
}
