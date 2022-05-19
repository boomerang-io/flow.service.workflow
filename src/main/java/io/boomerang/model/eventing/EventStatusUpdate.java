package io.boomerang.model.eventing;

import java.io.IOException;
import java.time.ZoneOffset;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import org.apache.http.entity.ContentType;
import io.boomerang.mongo.model.TaskStatus;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

public class EventStatusUpdate extends Event {

  protected static final String EXTENSION_ATTRIBUTE_CONTEXT = "initiatorcontext";

  private String workflowId;

  private String workflowActivityId;

  private TaskStatus status;

  private JsonNode initiatorContext;

  public EventStatusUpdate() {}

  public CloudEvent toCloudEvent() throws IOException {

    JsonObject jsonData = new JsonObject();
    jsonData.addProperty("workflowid", workflowId);
    jsonData.addProperty("workflowactivityid", workflowActivityId);
    jsonData.addProperty("status", status.toString());

    // @formatter:off
    CloudEvent cloudEvent = CloudEventBuilder.v03()
        .withId(getId())
        .withSource(getSource())
        .withSubject(getSubject())
        .withType(EVENT_TYPE_PREFIX + getType().toString().toLowerCase())
        .withTime(getDate().toInstant().atOffset(ZoneOffset.UTC))
        .withData(ContentType.APPLICATION_JSON.toString(), jsonData.toString().getBytes())
        .withExtension(EXTENSION_ATTRIBUTE_CONTEXT, initiatorContext.binaryValue())
        .build();
    // @formatter:on

    return cloudEvent;
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

  public TaskStatus getStatus() {
    return this.status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public JsonNode getInitiatorContext() {
    return this.initiatorContext;
  }

  public void setInitiatorContext(JsonNode initiatorContext) {
    this.initiatorContext = initiatorContext;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " workflowId='" + getWorkflowId() + "'" +
      ", workflowActivityId='" + getWorkflowActivityId() + "'" +
      ", status='" + getStatus() + "'" +
      ", initiatorContext='" + getInitiatorContext() + "'" +
      "}";
  }
  // @formatter:on
}
