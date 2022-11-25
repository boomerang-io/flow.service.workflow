package io.boomerang.model.eventing;

import java.text.MessageFormat;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.cloudevents.CloudEvent;

public class EventCancel extends Event {

  private static final String SUBJECT_PATTERN = "\\/workflow\\/(\\w+)\\/activity\\/(\\w+)";

  private String workflowId;

  private String workflowActivityId;

  public static Event fromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    // Identify the type of event (it must be of type "cancel")
    EventType eventType = EventType.valueOfCloudEventType(cloudEvent.getType());

    if (eventType != EventType.CANCEL) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Cloud event type must be \"{0}\" but is \"{1}\"!",
              EventType.CANCEL.getCloudEventType(), cloudEvent.getType()));
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

    // Map workflow ID and activity ID from the subject by using regex pattern
    Matcher matcher = Pattern.compile(SUBJECT_PATTERN).matcher(eventCancel.getSubject());

    if (!matcher.find() || matcher.groupCount() != 2) {
      throw new InvalidPropertiesFormatException(
          "For cancel cloud event types, the subject must have the format: \"/workflow/<workflow_id>/activity/<activity_id>\"");
    }

    eventCancel.setWorkflowId(matcher.group(1));
    eventCancel.setWorkflowActivityId(matcher.group(2));

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
