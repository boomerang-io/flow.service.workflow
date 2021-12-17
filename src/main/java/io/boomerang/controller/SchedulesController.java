package io.boomerang.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.TemplateWorkflowSummary;
import io.boomerang.model.UserWorkflowSummary;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.service.crud.WorkflowScheduleService;
import io.boomerang.service.crud.WorkflowService;

@RestController
@RequestMapping("/workflow/schedules")
public class SchedulesController {

  @Autowired
  private WorkflowScheduleService workflowScheduleService;
  
//  @DeleteMapping(value = "/{scheduleId}")
//  public void deleteWorkflowWithId(@PathVariable String scheduleId) {
//    workflowScheduleService.deleteSchedule(scheduleId);
//  }
}
