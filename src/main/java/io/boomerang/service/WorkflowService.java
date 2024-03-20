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

  Workflow get(String team, String workflowId, Optional<Integer> version, boolean withTasks);

  WorkflowResponsePage query(String queryTeam, Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryWorkflows);

  Workflow create(String team, Workflow request);

  Workflow apply(String team, Workflow workflow, boolean replace);

  void delete(String team, String workflowId);

  ResponseEntity<InputStreamResource> export(String team, String workflowId);

  Workflow duplicate(String team, String workflowId);

  WorkflowCanvas composeGet(String team, String workflowId, Optional<Integer> version);

  WorkflowCanvas composeApply(String team, WorkflowCanvas canvas, boolean replace);

  List<String> getAvailableParameters(String team, String workflowId);

  ResponseEntity<List<ChangeLogVersion>> changelog(String team, String workflowId);

  WorkflowCount count(String queryTeam, Optional<Long> from, Optional<Long> to, Optional<List<String>> queryLabels,
      Optional<List<String>> queryWorkflows);

  WorkflowRun submit(String team, String workflowId, WorkflowSubmitRequest request,
      boolean start);
}
