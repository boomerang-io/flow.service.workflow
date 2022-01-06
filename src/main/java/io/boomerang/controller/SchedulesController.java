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
import io.boomerang.model.ListActivityResponse;
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
  public ListActivityResponse getSchedules(
      @RequestParam Optional<List<String>> scopes, 
      @RequestParam Optional<List<String>> workflowIds,
      @RequestParam Optional<List<String>> teamIds,  
      @RequestParam Optional<Long> fromDate,
      @RequestParam Optional<Long> toDate, 
      @RequestParam Optional<List<String>> statuses,
      @RequestParam Optional<List<String>> types,
      @RequestParam Optional<List<String>> labels) {

//    TODO
  }
  
  @GetMapping(value = "")
  public List<WorkflowSchedule> getSchedules(@PathVariable String workflowId) {
    return workflowScheduleService.getSchedules(workflowId);
  }
  
  @GetMapping(value = "/calendar")
  public List<WorkflowScheduleCalendar> getSchedulesForDates(@PathVariable String workflowId, @RequestParam Long fromDate, @RequestParam Long toDate) {
    if (fromDate != null && toDate != null) {
      Date from = new Date(fromDate * 1000);
      Date to = new Date(toDate * 1000);
      return workflowScheduleService.getSchedulesForDates(workflowId, from, to);
    } else {
      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
    }
  }
  
  @PostMapping(value = "/schedule")
  public WorkflowSchedule createSchedule(@RequestBody WorkflowSchedule schedule) {
    return workflowScheduleService.createSchedule(schedule);
  }
  
  @GetMapping(value = "/schedule/{scheduleId}")
  public WorkflowSchedule getSchedule(@PathVariable String scheduleId) {
    return workflowScheduleService.getSchedule(scheduleId);
  }
  
  @GetMapping(value = "/schedule/{scheduleId}/calendar")
  public List<Date> getScheduleForDates(@PathVariable String scheduleId, @RequestParam Long fromDate, @RequestParam Long toDate) {
    if (fromDate != null && toDate != null) {
      Date from = new Date(fromDate * 1000);
      Date to = new Date(toDate * 1000);
      return workflowScheduleService.getScheduleForDates(scheduleId, from, to);
    } else {
      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
    }
  }
  
  @PatchMapping(value = "/schedule/{scheduleId}")
  public WorkflowSchedule updateSchedule(@PathVariable String scheduleId, @RequestBody WorkflowSchedule schedule) {
    return workflowScheduleService.updateSchedule(scheduleId, schedule);
  }
  
  @DeleteMapping(value = "/schedule/{scheduleId}")
  public ResponseEntity<?> updateSchedule(@PathVariable String scheduleId) {
    return workflowScheduleService.deleteSchedule(scheduleId);
  }
}
