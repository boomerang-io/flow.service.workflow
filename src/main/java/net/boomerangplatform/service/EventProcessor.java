package net.boomerangplatform.service;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;

public interface EventProcessor {

  void processHTTPEvent(Map<String, Object> headers, JsonNode payload);

  void processJSONMessage(String payload);
}
