package io.boomerang.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.boomerang.model.TemplateWorkflowSummary;
import io.boomerang.model.UserWorkflowSummary;
import io.boomerang.model.WorkflowSummary;
import io.boomerang.service.crud.WorkflowService;

@RestController
@RequestMapping("/workflow/workflows")
public class WorkflowsController {
  
  @Autowired
  private WorkflowService workflowService;
  
  @GetMapping(value = "system")
  public List<WorkflowSummary> getSystemWorkflows() {
    return workflowService.getSystemWorkflows();
  }
  
  @GetMapping(value = "user")
  public UserWorkflowSummary getUserWorkflows() {
    return workflowService.getUserWorkflows();
  }
  
  @GetMapping(value = "template")
  public List<TemplateWorkflowSummary> getTemplateWorkflows() {
    return workflowService.getTemplateWorkflows();
  }
  
  @GetMapping(value = "/{teamId}")
  public List<WorkflowSummary> getTeamWorkflows(@PathVariable String teamId) {
    return workflowService.getWorkflowsForTeam(teamId);
  }

}
