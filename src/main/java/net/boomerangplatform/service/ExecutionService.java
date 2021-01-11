package net.boomerangplatform.service;

import java.util.List;
import java.util.Optional;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.model.controller.TaskWorkspace;

public interface ExecutionService {
  public FlowActivity executeWorkflow(String workflowId,
      Optional<String> trigger,
      Optional<FlowExecutionRequest> executionRequest, Optional<List<TaskWorkspace>> taskWorkspaces);
}
