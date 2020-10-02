package net.boomerangplatform.service;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;

public interface EventProcessor {

  void processEvent(Map<String, Object> headers, JsonNode payload);

  void processMessage(String payload);
}
