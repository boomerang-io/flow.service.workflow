package io.boomerang.service;

import java.util.List;
import java.util.Optional;

public interface FilterService {

  List<String> getFilteredWorkflowIds(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes);

}
