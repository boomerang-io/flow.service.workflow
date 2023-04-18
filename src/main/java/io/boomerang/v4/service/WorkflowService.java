package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.client.WorkflowResponsePage;
import io.boomerang.v4.model.WorkflowCanvas;
import io.boomerang.v4.model.ref.Workflow;

public interface WorkflowService {

  ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version, boolean withTasks);

  ResponseEntity<Workflow> create(Workflow workflow, Optional<String> owner);

  ResponseEntity<Workflow> apply(Workflow workflow, boolean replace);

  ResponseEntity<WorkflowCanvas> compose(String workflowId, Optional<Integer> version);

  ResponseEntity<Void> enable(String workflowId);
  
  ResponseEntity<Void> disable(String workflowId);
  
  ResponseEntity<Void> delete(String workflowId);

  WorkflowResponsePage query(int page, int limit, Sort sort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryTeams, Optional<List<String>> workflows);

  ResponseEntity<InputStreamResource> export(String workflowId);

  ResponseEntity<Workflow> duplicate(String workflowId);

  List<String> getAvailableParameters(String workflowId);
}
