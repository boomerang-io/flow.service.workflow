package io.boomerang.model.eventing;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.UUID;
import io.boomerang.mongo.entity.ActivityEntity;
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

  public static EventStatusUpdate buildStatusUpdateFromActivity(ActivityEntity activityEntity) {
    String workflowId = activityEntity.getWorkflowId();
    String workflowActivityId = activityEntity.getId();
    String newStatus = activityEntity.getStatus().toString().toLowerCase();

    // Event subject
    String eventSubject =
        MessageFormat.format("/{0}/{1}/{2}", workflowId, workflowActivityId, newStatus);

    // Create status update event
    EventStatusUpdate eventStatusUpdate = new EventStatusUpdate();
    eventStatusUpdate.setId(UUID.randomUUID().toString());
    eventStatusUpdate.setSource(URI.create(EventFactory.class.getCanonicalName()));
    eventStatusUpdate.setSubject(eventSubject);
    eventStatusUpdate.setDate(new Date());
    eventStatusUpdate.setType(EventType.STATUS_UPDATE);
    eventStatusUpdate.setWorkflowId(activityEntity.getWorkflowId());
    eventStatusUpdate.setWorkflowActivityId(activityEntity.getId());
    eventStatusUpdate.setStatus(activityEntity.getStatus());
    eventStatusUpdate.setOutputProperties(activityEntity.getOutputProperties());
    eventStatusUpdate.setErrorResponse(activityEntity.getError());

    return eventStatusUpdate;
  }
}
