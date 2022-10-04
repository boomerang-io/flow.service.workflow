package io.boomerang.model.eventing;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;

public class EventTrigger extends Event {

  private static final String SUBJECT_PATTERN = "\\/workflow\\/(\\w+)\\/topic\\/(.+)";

  private String workflowId;

  private String topic;

  private String initiatorId;

  private String initiatorContext;

  private Map<String, String> properties;

  public static Event fromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    // Identify the type of event (it must be of type "trigger")
    EventType eventType = EventType.valueOfCloudEventType(cloudEvent.getType());

    if (eventType != EventType.TRIGGER) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Cloud event type must be \"{0}\" but is \"{1}\"!",
              EventType.TRIGGER.getCloudEventType(), cloudEvent.getType()));
    }

    // Create trigger event object and set base properties
    EventTrigger eventTrigger = new EventTrigger();
    eventTrigger.setId(cloudEvent.getId());
    eventTrigger.setSource(cloudEvent.getSource());
    eventTrigger.setSubject(cloudEvent.getSubject());
    eventTrigger.setToken(Optional.ofNullable(cloudEvent.getExtension(EXTENSION_ATTRIBUTE_TOKEN))
        .orElseGet(() -> "").toString());
    eventTrigger.setDate(new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    eventTrigger.setType(eventType);

    // Map workflow ID and topic from the subject by using regex pattern
    Matcher matcher = Pattern.compile(SUBJECT_PATTERN).matcher(eventTrigger.getSubject());

    if (!matcher.find() || matcher.groupCount() != 2) {
      throw new InvalidPropertiesFormatException(
          "For trigger cloud event types, the subject must have the format: \"/workflow/<workflow_id>/topic/<ce_topic>\"");
    }

    eventTrigger.setWorkflowId(matcher.group(1));
    eventTrigger.setTopic(matcher.group(2));

    // Map the properties
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> properties = new HashMap<>();
    PojoCloudEventData<JsonNode> cloudEventData = CloudEventUtils.mapData(cloudEvent,
        PojoCloudEventDataMapper.from(objectMapper, JsonNode.class));

    if (cloudEventData != null) {
      cloudEventData.getValue().fields()
          .forEachRemaining(entry -> properties.put(entry.getKey(), entry.getValue().toString()));
    }

    eventTrigger.setProperties(properties);
    eventTrigger.processExtensions(cloudEvent, eventTrigger);

    return eventTrigger;
  }

  public String getWorkflowId() {
    return this.workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getTopic() {
    return this.topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public String getInitiatorId() {
    return this.initiatorId;
  }

  public void setInitiatorId(String initiatorId) {
    this.initiatorId = initiatorId;
  }

  public String getInitiatorContext() {
    return this.initiatorContext;
  }

  public void setInitiatorContext(String initiatorContext) {
    this.initiatorContext = initiatorContext;
  }

  public Map<String, String> getProperties() {
    return this.properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  private EventTrigger processExtensions(CloudEvent cloudEvent, EventTrigger eventTrigger)
      throws InvalidPropertiesFormatException {

    // Map initiator ID and the context
    Object initiatorIdObj = cloudEvent.getExtension(EXTENSION_ATTRIBUTE_INITIATOR_ID);
    Object contextObject = cloudEvent.getExtension(EXTENSION_ATTRIBUTE_CONTEXT);

    if (initiatorIdObj != null) {
      if (initiatorIdObj.toString().matches("^[a-zA-Z0-9]+$")) {
        eventTrigger.setInitiatorId(initiatorIdObj.toString());
      } else {
        throw new InvalidPropertiesFormatException("Initiator ID must be alphanumeric!");
      }
    }

    if (contextObject != null) {
      eventTrigger.setInitiatorContext(contextObject.toString());
    }
    return eventTrigger;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " workflowId='" + getWorkflowId() + "'" +
      ", topic='" + getTopic() + "'" +
      ", initiatorId='" + getInitiatorId() + "'" +
      ", initiatorContext='" + getInitiatorContext() + "'" +
      ", properties='" + getProperties() + "'" +
      "}";
  }
  // @formatter:on
}
