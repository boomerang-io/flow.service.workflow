package io.boomerang.model.eventing;

import java.text.MessageFormat;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Optional;
import io.cloudevents.CloudEvent;

public class EventCancel extends Event {

  private String workflowId;

  private String workflowActivityId;

  public EventCancel() {}

  public static EventCancel fromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    // Identify the type of event (it must be of type "cancel")
    EventType eventType;

    try {
      String eventTypeString = cloudEvent.getType().replace(EVENT_TYPE_PREFIX, "").toUpperCase();
      eventType = EventType.valueOf(eventTypeString);
    } catch (Exception e) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Invalid cloud event type : \"{0}\"!", cloudEvent.getType()));
    }

    if (eventType != EventType.CANCEL) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Cloud event type must be \"{0}\" but is \"{0}\"!",
              EVENT_TYPE_PREFIX + EventType.CANCEL.toString().toLowerCase(), cloudEvent.getType()));
    }

    // Create cancel event object and set base properties
    EventCancel eventCancel = new EventCancel();
    eventCancel.setId(cloudEvent.getId());
    eventCancel.setSource(cloudEvent.getSource());
    eventCancel.setSubject(cloudEvent.getSubject());
    eventCancel.setToken(Optional.ofNullable(cloudEvent.getExtension(EXTENSION_ATTRIBUTE_TOKEN))
        .orElseGet(() -> "").toString());
    eventCancel.setDate(new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    eventCancel.setType(eventType);

    // Map workflow ID and activity ID from the subject
    String[] subjectTokens = eventCancel.getSubject().trim().replaceFirst("^/", "").split("/");

    if (subjectTokens.length != 2) {
      throw new InvalidPropertiesFormatException(
          "For cancel cloud event types, the subject must have the format: \"/<workflow_id>/<workflow_activity_id>\"");
    }

    eventCancel.workflowId = subjectTokens[0];
    eventCancel.workflowActivityId = subjectTokens[1];

    return eventCancel;
  }

  public String getWorkflowId() {
    return this.workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public String getWorkflowActivityId() {
    return this.workflowActivityId;
  }

  public void setWorkflowActivityId(String workflowActivityId) {
    this.workflowActivityId = workflowActivityId;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " workflowId='" + getWorkflowId() + "'" +
      ", workflowActivityId='" + getWorkflowActivityId() + "'" +
      "}";
  }
  // @formatter:on
}
