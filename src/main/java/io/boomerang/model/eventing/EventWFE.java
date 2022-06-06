package io.boomerang.model.eventing;

import java.text.MessageFormat;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Optional;
import io.boomerang.mongo.model.TaskStatus;
import io.cloudevents.CloudEvent;

public class EventWFE extends Event {

  protected static final String EXTENSION_ATTRIBUTE_STATUS = "status";

  private String workflowId;

  private String workflowActivityId;

  private String topic;

  private TaskStatus status;

  public EventWFE() {}

  public static Event fromCloudEvent(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    // Identify the type of event (it must be of type "wait for event")
    EventType eventType;

    try {
      String eventTypeString = cloudEvent.getType().replace(EVENT_TYPE_PREFIX, "").toUpperCase();
      eventType = EventType.valueOf(eventTypeString);
    } catch (Exception e) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Invalid cloud event type : \"{0}\"!", cloudEvent.getType()));
    }

    if (eventType != EventType.WFE) {
      throw new InvalidPropertiesFormatException(
          MessageFormat.format("Cloud event type must be \"{0}\" but is \"{0}\"!",
              EVENT_TYPE_PREFIX + EventType.WFE.toString().toLowerCase(), cloudEvent.getType()));
    }

    // Create WFE event object and set base properties
    EventWFE eventWFE = new EventWFE();
    eventWFE.setId(cloudEvent.getId());
    eventWFE.setSource(cloudEvent.getSource());
    eventWFE.setSubject(cloudEvent.getSubject());
    eventWFE.setToken(Optional.ofNullable(cloudEvent.getExtension(EXTENSION_ATTRIBUTE_TOKEN))
        .orElseGet(() -> "").toString());
    eventWFE.setDate(new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    eventWFE.setType(eventType);

    // Map workflow ID, activity ID and topic from the subject
    String[] subjectTokens = eventWFE.getSubject().trim().replaceFirst("^/", "").split("/");

    if (subjectTokens.length != 3) {
      throw new InvalidPropertiesFormatException(
          "For WFE cloud event types, the subject must have the format: \"/<workflow_id>/<workflow_activity_id>/<wfe_tasks_topic>\"");
    }

    eventWFE.setWorkflowId(subjectTokens[0]);
    eventWFE.setWorkflowActivityId(subjectTokens[1]);
    eventWFE.setTopic(subjectTokens[2]);

    // Get status
    TaskStatus status = TaskStatus.completed;

    try {
      String statusString = cloudEvent.getExtension(EXTENSION_ATTRIBUTE_STATUS).toString();

      switch (statusString.toLowerCase()) {
        case "success":
        case "completed":
          status = TaskStatus.completed;
          break;
        case "failure":
        case "failed":
        case "fail":
          status = TaskStatus.failure;
          break;
        default:
          break;
      }
    } catch (Exception e) {
    }

    eventWFE.setStatus(status);

    return eventWFE;
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

  public String getTopic() {
    return this.topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  public TaskStatus getStatus() {
    return this.status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " workflowId='" + getWorkflowId() + "'" +
      ", workflowActivityId='" + getWorkflowActivityId() + "'" +
      ", topic='" + getTopic() + "'" +
      ", status='" + getStatus() + "'" +
      "}";
  }
  // @formatter:on
}
