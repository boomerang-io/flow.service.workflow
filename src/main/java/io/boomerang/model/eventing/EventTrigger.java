package io.boomerang.model.eventing;

import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.util.LabelValueCodec;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;

public class EventTrigger extends Event {

  protected static final String EXTENSION_ATTRIBUTE_INITIATOR_ID = "initiatorid";

  protected static final String EXTENSION_ATTRIBUTE_CONTEXT = "initiatorcontext";

  private String workflowId;

  private String topic;

  private String initiatorId;

  private String initiatorContext;

  private Map<String, String> properties;

  public EventTrigger() {}

  public static EventTrigger fromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    // Identify the type of event (it must be of type "trigger")
    EventType eventType;

    try {
      String eventTypeString = cloudEvent.getType().replace(EVENT_TYPE_PREFIX, "").toUpperCase();
      eventType = EventType.valueOf(eventTypeString);
    } catch (Exception e) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Invalid cloud event type : \"{0}\"!", cloudEvent.getType()));
    }

    if (eventType != EventType.TRIGGER) {
      throw new InvalidPropertiesFormatException(MessageFormat.format(
          "Cloud event type must be \"{0}\" but is \"{0}\"!",
          EVENT_TYPE_PREFIX + EventType.TRIGGER.toString().toLowerCase(), cloudEvent.getType()));
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

    // Map workflow ID and topic from the subject
    String[] subjectTokens = eventTrigger.getSubject().trim().replaceFirst("^/", "").split("/");

    if (subjectTokens.length != 2) {
      throw new InvalidPropertiesFormatException(
          "For trigger cloud event types, the subject must have the format: \"/<workflow_id>/<workflow_custom_topic>\"");
    }

    eventTrigger.setWorkflowId(subjectTokens[0]);
    eventTrigger.setTopic(subjectTokens[1]);

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

	private EventTrigger processExtensions(CloudEvent ce, EventTrigger et) throws InvalidPropertiesFormatException {
		// Map initiator ID and the context
		Object initiatorIdObj = ce.getExtension(EXTENSION_ATTRIBUTE_INITIATOR_ID);
		Object contextObject = ce.getExtension(EXTENSION_ATTRIBUTE_CONTEXT);

		if (initiatorIdObj != null) {
			if (initiatorIdObj.toString().matches("^[a-zA-Z0-9]+$")) {
				et.setInitiatorId(initiatorIdObj.toString());
			} else {
				throw new InvalidPropertiesFormatException("Initiator ID must be alphanumeric!");
			}
		}

		if (contextObject != null) {
			et.setInitiatorContext(LabelValueCodec.encode(contextObject.toString()));
		}
		return et;
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
