package net.boomerangplatform.controller.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.WorkflowShortSummary;
import net.boomerangplatform.security.interceptors.AuthenticationScope;
import net.boomerangplatform.security.interceptors.Scope;
import net.boomerangplatform.service.crud.WorkflowService;

@RestController
@RequestMapping("/workflow/apis/v1")
public class WorkflowsV1Controller {

  @Autowired
  private WorkflowService workflowService;
  
  @GetMapping(value = "/workflows")
  @AuthenticationScope(scopes = {Scope.global})
  public List<WorkflowShortSummary> getAllWorkflows() {
    return workflowService.getWorkflowShortSummaryList();
  }

  @GetMapping(value = "/system-workflows")
  @AuthenticationScope(scopes = {Scope.global})
  public List<WorkflowShortSummary> getAllSystemworkflows() {
    return workflowService.getSystemWorkflowShortSummaryList();
  }
  
}
