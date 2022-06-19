package io.boomerang.extensions;

import java.net.URISyntaxException;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.JsonNode;

public interface SlackExtension {

  Supplier<Boolean> executeRunModal(JsonNode payload);

  ResponseEntity<?> handleAuth(String code);

  Supplier<Boolean> createRunModal(MultiValueMap<String, String> slackEvent);

  Supplier<Boolean> appHomeOpened(JsonNode payload);

  ResponseEntity<?> installRedirect() throws URISyntaxException;
  
}
