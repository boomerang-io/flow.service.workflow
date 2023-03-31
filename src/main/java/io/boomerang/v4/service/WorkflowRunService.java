package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.client.WorkflowRunResponsePage;
import io.boomerang.v4.model.ref.WorkflowRun;
import io.boomerang.v4.model.ref.WorkflowRunRequest;

public interface WorkflowRunService {

  WorkflowRunResponsePage query(int page, int limit, Sort sort, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> phase);

  ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks);

  ResponseEntity<WorkflowRun> submit(String workflowId, Optional<Integer> version, boolean start,
      Optional<WorkflowRunRequest> optRunRequest);

  ResponseEntity<WorkflowRun> start(String workflowRunId,
      Optional<WorkflowRunRequest> optRunRequest);

  ResponseEntity<WorkflowRun> finalize(String workflowRunId);

  ResponseEntity<WorkflowRun> cancel(String workflowRunId);

  ResponseEntity<WorkflowRun> retry(String workflowRunId);
  
}
