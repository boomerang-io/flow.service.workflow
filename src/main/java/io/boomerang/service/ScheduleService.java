package io.boomerang.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.WorkflowScheduleCalendar;

public interface ScheduleService {

  //Global method - no team
  CronValidationResponse validateCron(String cronString);

  //Team methods
  WorkflowSchedule get(String team, String scheduleId);

  Page<WorkflowSchedule> query(String queryTeam, int page, int limit, Sort sort,
      Optional<List<String>> queryStatus, Optional<List<String>> queryTypes,
      Optional<List<String>> queryWorkflows);

  List<WorkflowScheduleCalendar> calendars(String team, List<String> scheduleIds, Date fromDate,
      Date toDate);

  WorkflowSchedule create(String team, WorkflowSchedule schedule);

  void delete(String team, String scheduleId);

  WorkflowSchedule apply(String team, WorkflowSchedule request);
  List<WorkflowScheduleCalendar> getCalendarsForWorkflow(String team, String workflowId, Date fromDate,
      Date toDate);

}
