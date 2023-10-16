package io.boomerang.integrations.service;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;

public interface SlackService {

  Supplier<Boolean> executeRunModal(JsonNode payload);

  ResponseEntity<?> handleAuth(String code);

  Supplier<Boolean> createRunModal(Map<String, String> result);

  Supplier<Boolean> appHomeOpened(JsonNode payload);

  ResponseEntity<?> installRedirect() throws URISyntaxException;

  Boolean verifySignature(String signature, String timestamp, String body);

  Supplier appUninstalled(JsonNode payload);
  
}
