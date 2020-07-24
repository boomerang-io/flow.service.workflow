package net.boomerangplatform.service;

import java.util.concurrent.CompletableFuture;

public interface FlowExecutionService {
  CompletableFuture<Boolean> executeWorkflowVersion(String workFlowId, String activityId);
}
