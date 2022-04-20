package io.boomerang.service;

import io.boomerang.mongo.entity.ActivityEntity;

public interface EventingService {

  /*
   * Loop through a Workflow's parameters and if a JsonPath is set read the event payload and
   * attempt to find a payload.
   * 
   * Notes: - We drop exceptions to ensure Workflow continues executing - We return null if path not
   * found using DEFAULT_PATH_LEAF_TO_NULL.
   * 
   * Reference: - https://github.com/json-path/JsonPath#tweaking-configuration
   */
  void publishWorkflowActivityStatusUpdateCE(ActivityEntity activityEntity);
}
