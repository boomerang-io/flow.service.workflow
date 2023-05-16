package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort.Direction;
import io.boomerang.v4.model.WorkflowTemplate;

public interface WorkflowService {

  WorkflowTemplate get(String workflowId, Optional<Integer> version, boolean withTasks);

  Page<WorkflowTemplate> query(Optional<Integer> queryLimit, Optional<Integer> queryPage,
      Optional<Direction> querySort, Optional<List<String>> queryLabels,
      Optional<List<String>> queryStatus, Optional<List<String>> queryNames);

  WorkflowTemplate create(WorkflowTemplate request);
}
