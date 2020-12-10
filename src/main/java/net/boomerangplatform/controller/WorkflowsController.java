package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.WorkflowSummary;
import net.boomerangplatform.service.crud.WorkflowService;

@RestController
@RequestMapping("/workflow/workflows")
public class WorkflowsController {
  
  @Autowired
  private WorkflowService workflowService;
  
  @GetMapping(value = "system")
  public List<WorkflowSummary> getSystemWorkflows() {
    return workflowService.getSystemWorkflows();
  }
}
