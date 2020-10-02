package net.boomerangplatform.service;

import java.util.Optional;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;

public interface ExecutionService {
  public FlowActivity executeWorkflow(String workflowId,
      Optional<FlowTriggerEnum> trigger,
      Optional<FlowExecutionRequest> executionRequest);
}
