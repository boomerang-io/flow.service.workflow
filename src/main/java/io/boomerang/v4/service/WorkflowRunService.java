package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.model.ref.WorkflowRun;

public interface WorkflowRunService {

  Page<io.boomerang.v4.data.entity.ref.WorkflowRunEntity> query(int page, int limit, Sort sort, Optional<List<String>> labels,
      Optional<List<String>> status, Optional<List<String>> phase);

  ResponseEntity<WorkflowRun> get(String workflowRunId, boolean withTasks);
  
}
