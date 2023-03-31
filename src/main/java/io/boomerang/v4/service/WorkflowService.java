package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.model.GenerateTokenResponse;
import io.boomerang.v4.client.WorkflowResponsePage;
import io.boomerang.v4.model.WorkflowCanvas;
import io.boomerang.v4.model.ref.Workflow;

public interface WorkflowService {

  ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version, boolean withTasks);

  WorkflowResponsePage query(int page, int limit, Sort sort,
      Optional<List<String>> queryLabels, Optional<List<String>> queryStatus);  

  ResponseEntity<Workflow> create(Workflow workflow);

  ResponseEntity<Workflow> apply(Workflow workflow, boolean replace);

  ResponseEntity<WorkflowCanvas> compose(String workflowId, Optional<Integer> version);

  ResponseEntity<Void> enable(String workflowId);
  
  ResponseEntity<Void> disable(String workflowId);
  
  ResponseEntity<Void> delete(String workflowId);
}
