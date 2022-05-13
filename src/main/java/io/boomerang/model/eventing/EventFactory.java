package io.boomerang.model.eventing;

import java.text.MessageFormat;
import java.util.InvalidPropertiesFormatException;
import io.cloudevents.CloudEvent;

public class EventFactory {

  private static final String EVENT_TYPE_PREFIX = "io.boomerang.eventing.";

  public static Event buildFromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    // Identify the type of event
    EventType eventType;

    try {
      String eventTypeString = cloudEvent.getType().replace(EVENT_TYPE_PREFIX, "").toUpperCase();
      eventType = EventType.valueOf(eventTypeString);
    } catch (Exception e) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Invalid cloud event type : \"{0}\"!", cloudEvent.getType()));
    }

    switch (eventType) {
      case TRIGGER:
        return EventTrigger.fromCloudEvent(cloudEvent);
      case WFE:
        return EventWFE.fromCloudEvent(cloudEvent);
      case CANCEL:
        return EventCancel.fromCloudEvent(cloudEvent);
      default:
        return null;
    }
  }
}
