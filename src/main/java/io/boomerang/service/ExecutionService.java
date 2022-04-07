package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.controller.TaskWorkspace;

public interface ExecutionService {
  public FlowActivity executeWorkflow(String workflowId,
      Optional<String> trigger,
      Optional<FlowExecutionRequest> executionRequest, Optional<List<TaskWorkspace>> taskWorkspaces);
}
