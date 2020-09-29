package net.boomerangplatform.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.model.WorkflowShortSummary;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.service.crud.WorkflowService;
import net.boomerangplatform.service.refactor.TaskService;

@RestController
@RequestMapping("/internal")
public class InternalController {

  @Autowired
  private TaskService taskService;

  @Autowired
  private WorkflowService getAllWorkflows;

  @PostMapping(value = "/task/start")
  public void startTask(@RequestBody InternalTaskRequest request) {
    taskService.createTask(request);

  }

  @PostMapping(value = "/task/end")
  public void endTask(@RequestBody InternalTaskResponse request) {
    taskService.endTask(request);
  }


  @GetMapping(value = "/workflows")
  public List<WorkflowShortSummary> getAllWorkflows() {
    return getAllWorkflows.getWorkflowShortSummaryList();
  }
 
}
