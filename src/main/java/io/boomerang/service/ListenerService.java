package io.boomerang.service;

import java.util.Optional;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.ref.WorkflowRun;
import io.cloudevents.CloudEvent;

public interface ListenerService {

  ResponseEntity<WorkflowRun> processWebhook(String trigger, String workflowId,
      JsonNode payload);

  ResponseEntity<WorkflowRun> processWFE(String workflowId, String workflowRunId, String topic,
      String status, Optional<JsonNode> payload);

  ResponseEntity<?> processEvent(CloudEvent event);
}
