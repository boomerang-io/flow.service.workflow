package io.boomerang.model.eventing;

import java.net.URI;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.cloudevents.CloudEvent;

public class EventFactory {

  private static final String EVENT_SOURCE_URI = "/apis/v1/events";

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

  public static EventWorkflowStatusUpdate buildStatusUpdateEvent(ActivityEntity activityEntity) {

    // Event subject
    // @formatter:off
    String eventSubject = MessageFormat.format("/workflow/{0}/activity/{1}/status/{2}",
        activityEntity.getWorkflowId(),
        activityEntity.getId(),
        activityEntity.getStatus().toString().toLowerCase());
    // @formatter:off

    // Create workflow status update event
    EventWorkflowStatusUpdate eventStatusUpdate = new EventWorkflowStatusUpdate();
    eventStatusUpdate.setId(UUID.randomUUID().toString());
    eventStatusUpdate.setSource(URI.create(EVENT_SOURCE_URI));
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

  public static EventTaskStatusUpdate buildStatusUpdateEvent(
      TaskExecutionEntity taskExecutionEntity, Map<String, String> additionalData) {

    // Event subject
    // @formatter:off
    String eventSubject = MessageFormat.format("/workflow/{0}/activity/{1}/task/{2}/status/{3}",
        taskExecutionEntity.getWorkflowId(),
        taskExecutionEntity.getActivityId(),
        taskExecutionEntity.getId(),
        taskExecutionEntity.getFlowTaskStatus().toString().toLowerCase());
    // @formatter:on

    // Create task status update event
    EventTaskStatusUpdate eventStatusUpdate = new EventTaskStatusUpdate();
    eventStatusUpdate.setId(UUID.randomUUID().toString());
    eventStatusUpdate.setSource(URI.create(EVENT_SOURCE_URI));
    eventStatusUpdate.setSubject(eventSubject);
    eventStatusUpdate.setDate(new Date());
    eventStatusUpdate.setType(EventType.TASK_STATUS_UPDATE);
    eventStatusUpdate.setTaskId(taskExecutionEntity.getId());
    eventStatusUpdate.setWorkflowId(taskExecutionEntity.getWorkflowId());
    eventStatusUpdate.setWorkflowActivityId(taskExecutionEntity.getActivityId());
    eventStatusUpdate.setStatus(taskExecutionEntity.getFlowTaskStatus());
    eventStatusUpdate.setErrorResponse(taskExecutionEntity.getError());
    eventStatusUpdate.setAdditionalData(additionalData);
    eventStatusUpdate.setOutputProperties(
        Optional.ofNullable(taskExecutionEntity.getOutputs()).orElse(Collections.emptyMap())
            .entrySet().stream().map(entry -> new KeyValuePair(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList()));

    return eventStatusUpdate;
  }
}
