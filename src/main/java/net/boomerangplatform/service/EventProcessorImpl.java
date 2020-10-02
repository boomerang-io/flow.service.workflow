package net.boomerangplatform.service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import io.cloudevents.json.Json;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import net.boomerangplatform.client.NatsClient;

@Service
public class EventProcessorImpl implements EventProcessor {

  protected static final String TYPE_PREFIX = "io.boomerang.eventing.";
  
  protected static final String SUBJECT = "flow-workflow-execute";
  
  @Value("${eventing.enabled}")
  private Boolean eventingEnabled;

  private static final Logger LOGGER = LogManager.getLogger(EventProcessorImpl.class);

  @Autowired
  private NatsClient natsClient;
  
  @Autowired
//  private WorkflowClient wfClient;

  @Override
  public void routeEvent(String requestUri, String target, String workflowId, JsonNode payload) {
    final String eventId = UUID.randomUUID().toString();
    final String eventType = TYPE_PREFIX + target;
    final URI uri = URI.create(requestUri);
        
    final CloudEventImpl<JsonNode> cloudEvent =
    CloudEventBuilder.<JsonNode>builder()
      .withType(eventType)
      .withId(eventId)
      .withSource(uri)
      .withData(payload)
      .withSubject(workflowId)
      .withTime(ZonedDateTime.now())
      .build();

    final String jsonPayload = Json.encode(cloudEvent);
    LOGGER.info("CloudEvent Object - " + jsonPayload);
    if (eventingEnabled) {
//      natsClient.publishMessage(SUBJECT, jsonPayload);
    } else {
//      wfClient.executeWorkflowPut(SUBJECT, cloudEvent, workflowId);
    }
  }
}
