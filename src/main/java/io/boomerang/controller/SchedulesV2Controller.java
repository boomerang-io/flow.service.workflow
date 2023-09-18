package io.boomerang.controller;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.WorkflowScheduleCalendar;
import io.boomerang.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/schedules")
@Tag(name = "Schedule Management",
description = "Provide the ability to create and update Schedules.")
public class SchedulesV2Controller {

  @Autowired
  private ScheduleService workflowScheduleService;
  
  @GetMapping(value = "/validate/cron")
  @Operation(summary = "Validate a Schedules CRON.")
  public CronValidationResponse validateCron(@Parameter(name = "cron",
      description = "A CRON expression to validate",
      required = true) @RequestParam String cron) {
    return workflowScheduleService.validateCron(cron);
  }
  
  @GetMapping(value = "/{scheduleId}")
  @Operation(summary = "Retrieve a Schedule.")
  public WorkflowSchedule get(@PathVariable String scheduleId) {
    return workflowScheduleService.get(scheduleId);
  }
  
  @GetMapping(value = "/query")
  @Operation(summary = "Search for Schedules")
  public Page<WorkflowSchedule> query(
      @Parameter(name = "statuses", description = "List of statuses to filter for. Defaults to all.",
          example = "active,archived",
          required = false) @RequestParam(required = false) Optional<List<String>> statuses,
      @Parameter(name = "types", description = "List of types to filter for. Defaults to all.",
      example = "cron,advancedCron",
      required = false) @RequestParam(required = false) Optional<List<String>> types,
      @Parameter(name = "workflows", description = "List of workflows to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> workflows,
      @Parameter(name = "teams", description = "List of teams to filter for.", 
      required = false) @RequestParam(required = false) Optional<List<String>> teams,
      @Parameter(name = "limit", description = "Result Size", example = "10",
          required = true) @RequestParam(defaultValue = "10") int limit,
      @Parameter(name = "page", description = "Page Number", example = "0",
          required = true) @RequestParam(defaultValue = "0") int page) {
    final Sort sort = Sort.by(new Order(Direction.ASC, "creationDate"));
      return workflowScheduleService.query(page, limit, sort, statuses, types, workflows, teams);
  }
  
  @GetMapping(value = "/calendars")
  @Operation(summary = "Retrieve Calendars for Schedules by Dates.")
  public List<WorkflowScheduleCalendar> getCalendarsForSchedules(@RequestParam List<String> schedules, @RequestParam Long fromDate, @RequestParam Long toDate) {
    if (schedules != null && !schedules.isEmpty() && fromDate != null && toDate != null) {
      Date from = new Date(fromDate * 1000);
      Date to = new Date(toDate * 1000);
      return workflowScheduleService.calendars(schedules, from, to);
    } else {
      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
    }
  }
  
  @PostMapping(value = "")
  @Operation(summary = "Create a Schedule.")
  public WorkflowSchedule createSchedule(
      @Parameter(name = "team", description = "Team as owner reference.", example = "my-amazing-team",
      required = true) @RequestParam(required = true) String team,
      @RequestBody WorkflowSchedule schedule) {
    return workflowScheduleService.create(schedule, team);
  }
  
  @PutMapping(value = "")
  @Operation(summary = "Apply a Schedule.")
  public WorkflowSchedule updateSchedule(@RequestBody WorkflowSchedule schedule,
      @Parameter(name = "team", description = "Team as owner reference. Required if using apply to create new.",
      example = "my-amazing-team",
      required = false) @RequestParam(required = false) Optional<String> team) {
    return workflowScheduleService.apply(schedule, team);
  }
  
  @DeleteMapping(value = "/{scheduleId}")
  @Operation(summary = "Delete a Schedule.")
  public ResponseEntity<?> deleteSchedule(@PathVariable String scheduleId) {
    return workflowScheduleService.delete(scheduleId);
  }
  
  //TODO: commented out in Web - is this useful?
//  @GetMapping(value = "/{scheduleId}/calendar")
//  @Operation(summary = "Retrieve a Calendar based on dates.")
//  public List<Date> getScheduleForDates(@PathVariable String scheduleId, @RequestParam Long fromDate, @RequestParam Long toDate) {
//    if (fromDate != null && toDate != null) {
//      Date from = new Date(fromDate * 1000);
//      Date to = new Date(toDate * 1000);
//      return workflowScheduleService.getCalendarForDates(scheduleId, from, to);
//    } else {
//      throw new BoomerangException(0, "Invalid fromDate or toDate", HttpStatus.BAD_REQUEST);
//    }
//  }
}
