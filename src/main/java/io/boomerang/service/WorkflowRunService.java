package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.client.WorkflowRunResponsePage;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowRunCount;
import io.boomerang.model.ref.WorkflowRunInsight;
import io.boomerang.model.ref.WorkflowRunRequest;

public interface WorkflowRunService {

  WorkflowRunResponsePage query(String team, Optional<Long> fromDate, Optional<Long> toDate,
      Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> queryOrder,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryPhase, Optional<List<String>> queryWorkflowRuns,
      Optional<List<String>> queryWorkflows, Optional<List<String>> queryTriggers);

  ResponseEntity<WorkflowRun> get(String team, String workflowRunId, boolean withTasks);

  ResponseEntity<WorkflowRun> start(String team, String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest);

  ResponseEntity<WorkflowRun> finalize(String team, String workflowRunId);

  ResponseEntity<WorkflowRun> cancel(String team, String workflowRunId);

  ResponseEntity<WorkflowRun> retry(String team, String workflowRunId);

  WorkflowRunInsight insight(String team, Optional<Long> from, Optional<Long> to,
      Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows);

  WorkflowRunCount count(String team, Optional<Long> from, Optional<Long> to,
      Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows);
}
