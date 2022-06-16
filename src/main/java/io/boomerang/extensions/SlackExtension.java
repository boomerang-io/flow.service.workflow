package io.boomerang.extensions;

import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;

public interface SlackExtension {

  Supplier<Boolean> createRunModal(String triggerId, String userId, String teamId, String workflowId);

  Supplier<Boolean> executeRunModal(JsonNode payload);

  ResponseEntity<?> handleAuth(String code);
  
}
