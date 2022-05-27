package io.boomerang.model.eventing;

import java.io.IOException;
import java.time.ZoneOffset;
import com.google.gson.JsonObject;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.util.Strings;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.util.LabelValueCodec;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

public class EventStatusUpdate extends Event {

  protected static final String EXTENSION_ATTRIBUTE_CONTEXT = "initiatorcontext";

  private String workflowId;

  private String workflowActivityId;

  private TaskStatus status;

  private String initiatorContext;

  public EventStatusUpdate() {}

  public CloudEvent toCloudEvent() throws IOException {

    JsonObject jsonData = new JsonObject();
    jsonData.addProperty("workflowid", workflowId);
    jsonData.addProperty("workflowactivityid", workflowActivityId);
    jsonData.addProperty("status", status.toString());

    // @formatter:off
    CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v03()
        .withId(getId())
        .withSource(getSource())
        .withSubject(getSubject())
        .withType(EVENT_TYPE_PREFIX + getType().toString().toLowerCase())
        .withTime(getDate().toInstant().atOffset(ZoneOffset.UTC))
        .withData(ContentType.APPLICATION_JSON.toString(), jsonData.toString().getBytes());
    // @formatter:on

    if (Strings.isNotEmpty(initiatorContext)) {
      cloudEventBuilder = cloudEventBuilder.withExtension(EXTENSION_ATTRIBUTE_CONTEXT,
          LabelValueCodec.decode(initiatorContext));
    }

    return cloudEventBuilder.build();
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

  public String getInitiatorContext() {
    return this.initiatorContext;
  }

  public void setInitiatorContext(String initiatorContext) {
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
