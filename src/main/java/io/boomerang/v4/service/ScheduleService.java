package io.boomerang.v4.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.model.CronValidationResponse;
import io.boomerang.v4.model.WorkflowSchedule;
import io.boomerang.v4.model.WorkflowScheduleCalendar;

public interface ScheduleService {

  WorkflowSchedule get(String scheduleId);

  Page<WorkflowSchedule> query(int page, int limit, Sort sort,
      Optional<List<String>> queryWorkflows, Optional<List<String>> queryTeams,
      Optional<List<String>> queryStatus, Optional<List<String>> queryTypes);

  WorkflowSchedule create(WorkflowSchedule schedule, String teamId);

  ResponseEntity<?> delete(String scheduleId);

  WorkflowSchedule update(String scheduleId,
      WorkflowSchedule patchSchedule);

  ResponseEntity<?> complete(String scheduleId);

  CronValidationResponse validateCron(String cronString);

  List<Date> getCalendarForDates(String scheduleId, Date fromDate, Date toDate);

  void enableAllTriggerSchedules(String workflowId);

  void disableAllTriggerSchedules(String workflowId);

  void deleteAllForWorkflow(String workflowId) throws SchedulerException;

  List<WorkflowScheduleCalendar> getCalendarsForWorkflow(String workflowId, Date fromDate,
      Date toDate);

  List<WorkflowScheduleCalendar> getCalendarsForSchedules(List<String> scheduleIds, Date fromDate,
      Date toDate);
}
