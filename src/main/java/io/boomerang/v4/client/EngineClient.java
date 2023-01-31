package io.boomerang.v4.client;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunRequest;

public interface EngineClient {

  WorkflowRunResponsePage queryWorkflowRuns(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase,
      Optional<List<String>> queryIds);

  WorkflowRun getWorkflowRun(String workflowRunId, boolean withTasks);

  WorkflowRun submitWorkflowRun(String workflowId, Optional<Integer> version, boolean start,
      Optional<WorkflowRunRequest> request);

  WorkflowRun startWorkflowRun(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest);
}
