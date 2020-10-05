package net.boomerangplatform.service;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import io.cloudevents.v1.CloudEventImpl;
import net.boomerangplatform.model.eventing.EventResponse;

public interface EventProcessor {

  CloudEventImpl<EventResponse> processHTTPEvent(Map<String, Object> headers, JsonNode payload);

  void processNATSMessage(String payload);
}
