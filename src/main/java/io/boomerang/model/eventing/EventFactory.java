package io.boomerang.model.eventing;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.UUID;
import io.boomerang.mongo.entity.ActivityEntity;
import io.cloudevents.CloudEvent;

public class EventFactory {

  private EventFactory() {}

  public static Event buildFromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {
    EventType eventType = EventType.valueOfCloudEventType(cloudEvent.getType());

    switch (eventType) {
      case TRIGGER:
        return EventTrigger.fromCloudEvent(cloudEvent);
      case WFE:
        return EventWFE.fromCloudEvent(cloudEvent);
      case CANCEL:
        return EventCancel.fromCloudEvent(cloudEvent);
      default:
        throw new InvalidPropertiesFormatException(
            MessageFormat.format("Invalid cloud event type : \"{0}\"!", cloudEvent.getType()));
    }
  }

  public static EventWorkflowStatusUpdate buildWorkflowStatusUpdateFromActivity(
      ActivityEntity activityEntity) {
    String workflowId = activityEntity.getWorkflowId();
    String workflowActivityId = activityEntity.getId();
    String newStatus = activityEntity.getStatus().toString().toLowerCase();

    // Event subject
    String eventSubject =
        MessageFormat.format("/{0}/{1}/{2}", workflowId, workflowActivityId, newStatus);

    // Create status update event
    EventWorkflowStatusUpdate eventStatusUpdate = new EventWorkflowStatusUpdate();
    eventStatusUpdate.setId(UUID.randomUUID().toString());
    eventStatusUpdate.setSource(URI.create(EventFactory.class.getCanonicalName()));
    eventStatusUpdate.setSubject(eventSubject);
    eventStatusUpdate.setDate(new Date());
    eventStatusUpdate.setType(EventType.WORKFLOW_STATUS_UPDATE);
    eventStatusUpdate.setWorkflowId(activityEntity.getWorkflowId());
    eventStatusUpdate.setWorkflowActivityId(activityEntity.getId());
    eventStatusUpdate.setStatus(activityEntity.getStatus());
    eventStatusUpdate.setOutputProperties(activityEntity.getOutputProperties());
    eventStatusUpdate.setErrorResponse(activityEntity.getError());

    return eventStatusUpdate;
  }
}
