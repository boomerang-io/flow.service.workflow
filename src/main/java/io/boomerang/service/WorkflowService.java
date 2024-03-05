package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.ResponseEntity;
import io.boomerang.client.WorkflowResponsePage;
import io.boomerang.model.WorkflowCanvas;
import io.boomerang.model.ref.ChangeLogVersion;
import io.boomerang.model.ref.Workflow;
import io.boomerang.model.ref.WorkflowCount;
import io.boomerang.model.ref.WorkflowRun;
import io.boomerang.model.ref.WorkflowSubmitRequest;

public interface WorkflowService {

  Workflow get(String workflowId, Optional<Integer> version, boolean withTasks);

  WorkflowResponsePage query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryTeams,
      Optional<List<String>> queryWorkflows);

  Workflow create(Workflow request, String team);

  Workflow apply(Workflow workflow, boolean replace, Optional<String> team);

  void delete(String workflowId);

  ResponseEntity<InputStreamResource> export(String workflowId);

  Workflow duplicate(String workflowId);

  WorkflowCanvas composeGet(String workflowId, Optional<Integer> version);

  WorkflowCanvas composeApply(WorkflowCanvas canvas, boolean replace,
      Optional<String> team);

  List<String> getAvailableParameters(String workflowId);

  ResponseEntity<List<ChangeLogVersion>> changelog(String workflowId);

  WorkflowCount count(Optional<Long> from, Optional<Long> to, Optional<List<String>> queryLabels,
      Optional<List<String>> queryTeams, Optional<List<String>> queryWorkflows);

  WorkflowRun submit(String workflowId, WorkflowSubmitRequest request,
      boolean start);
  
}
