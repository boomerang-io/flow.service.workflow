package io.boomerang.controller;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.service.ExecutionService;

@RestController
@RequestMapping("/workflow/")
public class ExecutionController {

  @Autowired
  private ExecutionService executionService;

  @PostMapping(value = "/execute/{workflowId}")
  public FlowActivity executeWorkflow(@PathVariable String workflowId,
      @RequestParam Optional<String> trigger,
      @RequestBody Optional<FlowExecutionRequest> executionRequest) {

    return executionService.executeWorkflow(workflowId, trigger, executionRequest, Optional.empty());
  }
}
