package io.boomerang.v4.service;

import java.util.List;
import java.util.Optional;

public interface FilterServiceV4 {

  List<String> getFilteredWorkflowIdsForUserEmail(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes, String userEmail);

  List<String> getFilteredWorkflowIds(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes);
}
