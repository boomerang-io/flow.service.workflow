package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.client.WorkflowRunResponsePage;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunCount;
import io.boomerang.v4.model.ref.WorkflowRunInsight;
import io.boomerang.v4.model.ref.WorkflowRunRequest;
import io.boomerang.v4.model.ref.WorkflowRunSubmitRequest;

public interface WorkflowRunService {

  WorkflowRunResponsePage query(Optional<Long> fromDate, Optional<Long> toDate,
      Optional<Integer> queryLimit, Optional<Integer> queryPage, Optional<Direction> queryOrder,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus,
      Optional<List<String>> queryPhase, Optional<List<String>> queryTeams,
      Optional<List<String>> queryWorkflowRuns, Optional<List<String>> queryWorkflows,
      Optional<List<String>> queryTriggers);

  ResponseEntity<WorkflowRun> submit(WorkflowRunSubmitRequest request, boolean start);

  ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks);

  ResponseEntity<WorkflowRun> start(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest);

  ResponseEntity<WorkflowRun> finalize(String workflowRunId);

  ResponseEntity<WorkflowRun> cancel(String workflowRunId);

  ResponseEntity<WorkflowRun> retry(String workflowRunId);

  WorkflowRunInsight insight(Optional<Long> from, Optional<Long> to,
      Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows,
      Optional<List<String>> queryTeams);

  WorkflowRunCount count(Optional<Long> from, Optional<Long> to,
      Optional<List<String>> queryLabels, Optional<List<String>> queryWorkflows,
      Optional<List<String>> queryTeams);
  
}
