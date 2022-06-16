package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import io.boomerang.model.FlowActivity;
import io.boomerang.mongo.entity.ActivityEntity;

public interface FilterService {

  List<String> getFilteredWorkflowIds(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes);

  List<FlowActivity> convertActivityEntityToFlowActivity(List<ActivityEntity> records);

  List<String> getFilteredWorkflowIdsForUserEmail(Optional<List<String>> workflowIds,
      Optional<List<String>> teamIds, Optional<List<String>> scopes, String userEmail);
}
