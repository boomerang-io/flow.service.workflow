package io.boomerang.service;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.ref.WorkflowRun;
import io.cloudevents.CloudEvent;

public interface TriggerService {
  ResponseEntity<?> processGitHubWebhook(String trigger, String eventType, JsonNode payload);

  WorkflowRun processWFE(String workflowId, String workflowRunId, String topic,
      String status, Optional<JsonNode> payload);

  WorkflowRun processEvent(CloudEvent event, Optional<String> workflow);

  WorkflowRun processWebhook(String workflowId, JsonNode payload);
}
