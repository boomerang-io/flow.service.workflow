package net.boomerangplatform.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.service.refactor.TaskService;

@RestController
@RequestMapping("/internal")
public class InternalController {

  @Autowired
  private TaskService taskService;
  
  @PostMapping(value = "/task/start")
  public void startTask(@RequestBody InternalTaskRequest request) {
    taskService.createTask(request);

  }
  
  @PostMapping(value = "/task/end")
  public void endTask(@RequestBody InternalTaskResponse request) {
    taskService.endTask(request);
  }
}
