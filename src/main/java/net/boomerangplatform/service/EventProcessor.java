package net.boomerangplatform.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface EventProcessor {

  void routeEvent(String requestUri, String target, String workflowId, JsonNode payload);
}
