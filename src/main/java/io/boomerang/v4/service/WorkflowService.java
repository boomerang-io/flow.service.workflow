package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import io.boomerang.v4.data.entity.ref.WorkflowEntity;
import io.boomerang.v4.model.ref.Workflow;

public interface WorkflowService {

  ResponseEntity<Workflow> get(String workflowId, Optional<Integer> version, boolean withTasks);

  Page<WorkflowEntity> query(int page, int limit, Sort sort, Optional<List<String>> labels,
      Optional<List<String>> status);

  ResponseEntity<Workflow> create(Workflow workflow, boolean b);

  ResponseEntity<Workflow> apply(Workflow workflow, boolean replace);

  ResponseEntity<Workflow> archive(String workflowId);

  ResponseEntity<Workflow> compose(String workflowId);
  
}
