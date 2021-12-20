package io.boomerang.service.crud;

import java.util.Date;
import java.util.List;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.WorkflowSchedule;

public interface WorkflowScheduleService {

  void enableSchedule(String scheduleId) throws SchedulerException;

  void disableSchedule(String scheduleId) throws SchedulerException;

  void deleteAllSchedules(String workflowId) throws SchedulerException;

  WorkflowSchedule createSchedule(String workflowId, WorkflowSchedule schedule);

  List<WorkflowSchedule> getSchedules(String workflowId);

  WorkflowSchedule getSchedule(String workflowId, String scheduleId);

  ResponseEntity<?> deleteSchedule(String workflowId, String scheduleId);

  WorkflowSchedule updateSchedule(String workflowId, String scheduleId,
      WorkflowSchedule patchSchedule);

  CronValidationResponse validateCron(String cronString);

  List<Date> getSchedulesForDates(String workflowId, Date fromDate, Date toDate);

  List<Date> getScheduleForDates(String workflowId, String scheduleId, Date fromDate, Date toDate);

  void disableAllSchedules(String workflowId);

  void enableAllSchedules(String workflowId);
}
