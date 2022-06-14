package io.boomerang.extensions;

import java.util.function.Supplier;
import com.fasterxml.jackson.databind.JsonNode;

public interface SlackExtension {

  Supplier<Boolean> createRunModal(String triggerId, String userId, String workflowId);

  Supplier executeRunModal(JsonNode payload);
  
}
