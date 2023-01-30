package io.boomerang.v4.client;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import io.boomerang.v4.model.ref.WorkflowRun;

public interface EngineClient {

  WorkflowRunResponsePage queryWorkflowRuns(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryPhase,
      Optional<List<String>> queryIds);

  WorkflowRun getWorkflowRun(String workflowRunId, boolean withTasks);
}
