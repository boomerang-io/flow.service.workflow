package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.WorkflowScheduleCalendar;
import io.boomerang.service.crud.WorkflowScheduleService;

@RestController
@RequestMapping("/workflow/schedules")
public class SchedulesController {

  @Autowired
  private WorkflowScheduleService workflowScheduleService;
  
  @GetMapping(value = "/validate/cron")
  public CronValidationResponse validateCron(@RequestParam String cron) {
    return workflowScheduleService.validateCron(cron);
  }
  
  @GetMapping(value = "")
  public List<WorkflowSchedule> getSchedules(
      @RequestParam Optional<List<String>> scopes, 
      @RequestParam Optional<List<String>> workflowIds,
      @RequestParam Optional<List<String>> teamIds, 
      @RequestParam Optional<List<String>> statuses,
      @RequestParam Optional<List<String>> types) {
      return workflowScheduleService.getSchedules(workflowIds, teamIds, statuses,
          types, scopes);
  }
  
  @GetMapping(value = "/calendar")
  public List<WorkflowScheduleCalendar> getCalendarsForSchedules(@RequestParam List<String> scheduleIds, @RequestParam Long fromDate, @RequestParam Long toDate) {
    if (scheduleIds != null && !scheduleIds.isEmpty() && fromDate != null && toDate != null) {
      Date from = new Date(fromDate * 1000);
      Date to = new Date(toDate * 1000);
      return workflowScheduleService.getCalendarsForSchedules(scheduleIds, from, to);
    } else {
      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
    }
  }
  
  @PostMapping(value = "")
  public WorkflowSchedule createSchedule(@RequestBody WorkflowSchedule schedule) {
    return workflowScheduleService.createSchedule(schedule);
  }
  
  @GetMapping(value = "/{scheduleId}")
  public WorkflowSchedule getSchedule(@PathVariable String scheduleId) {
    return workflowScheduleService.getSchedule(scheduleId);
  }
  
  @GetMapping(value = "/{scheduleId}/calendar")
  public List<Date> getScheduleForDates(@PathVariable String scheduleId, @RequestParam Long fromDate, @RequestParam Long toDate) {
    if (fromDate != null && toDate != null) {
      Date from = new Date(fromDate * 1000);
      Date to = new Date(toDate * 1000);
      return workflowScheduleService.getCalendarForDates(scheduleId, from, to);
    } else {
      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
    }
  }
  
  @PatchMapping(value = "/{scheduleId}")
  public WorkflowSchedule updateSchedule(@PathVariable String scheduleId, @RequestBody WorkflowSchedule schedule) {
    return workflowScheduleService.updateSchedule(scheduleId, schedule);
  }
  
  @DeleteMapping(value = "/{scheduleId}")
  public ResponseEntity<?> updateSchedule(@PathVariable String scheduleId) {
    return workflowScheduleService.deleteSchedule(scheduleId);
  }
}
