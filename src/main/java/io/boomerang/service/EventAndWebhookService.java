package io.boomerang.service;

import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import io.boomerang.model.ref.WorkflowRun;

public interface EventAndWebhookService {

//  ResponseEntity<?> process(CloudEvent cloudEvent);

//  ResponseEntity<CloudEvent<AttributesImpl, JsonNode>> routeCloudEvent(CloudEvent<AttributesImpl, JsonNode> cloudEvent, String token, URI uri);
//
  ResponseEntity<WorkflowRun> processWebhook(String trigger, String workflowId,
      JsonNode payload, String workflowActivityId, String topic, String status);
}
