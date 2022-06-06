package io.boomerang.model.eventing;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.util.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.KeyValuePair;
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

  private List<KeyValuePair> outputProperties;

  private ErrorResponse errorResponse;

  private List<TaskExecutionResponse> executedTasks;

  public EventStatusUpdate() {}

  public CloudEvent toCloudEvent() throws IOException {

    JsonObject jsonData = new JsonObject();
    jsonData.addProperty("workflowid", workflowId);
    jsonData.addProperty("workflowactivityid", workflowActivityId);
    jsonData.addProperty("status", status.toString());

    // Configure and create Gson object
    Gson gson = new GsonBuilder().create();

    // Add output properties to JSON data
    if (outputProperties != null && outputProperties.isEmpty() == false) {
      jsonData.add("outputProperties", gson.toJsonTree(outputProperties));
    }

    // Add error data to JSON data
    if (errorResponse != null) {
      jsonData.add("error", gson.toJsonTree(errorResponse));
    }

    // Add task execution responses to JSON data
    if (executedTasks != null && executedTasks.isEmpty() == false) {
      JsonArray jsonTasks = new JsonArray();

      executedTasks.stream().forEach(task -> {
        JsonObject jsonTask = new JsonObject();
        jsonTask.addProperty("id", task.getId());
        jsonTask.addProperty("name", task.getTaskName());
        jsonTask.addProperty("type", task.getTaskType().toString());
        jsonTask.addProperty("status", task.getFlowTaskStatus().toString());

        // Set the error (if any)
        if (task.getError() != null) {
          jsonTask.add("error", gson.toJsonTree(task.getError()));
        }

        // Add task details to the array
        jsonTasks.add(jsonTask);
      });

      // Add JSON task array to JSON data
      jsonData.add("executedTasks", jsonTasks);
    }

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

  public List<KeyValuePair> getOutputProperties() {
    return this.outputProperties;
  }

  public void setOutputProperties(List<KeyValuePair> outputProperties) {
    this.outputProperties = outputProperties;
  }

  public ErrorResponse getErrorResponse() {
    return this.errorResponse;
  }

  public void setErrorResponse(ErrorResponse errorResponse) {
    this.errorResponse = errorResponse;
  }

  public List<TaskExecutionResponse> getExecutedTasks() {
    return this.executedTasks;
  }

  public void setExecutedTasks(List<TaskExecutionResponse> executedTasks) {
    this.executedTasks = executedTasks;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " workflowId='" + getWorkflowId() + "'" +
      ", workflowActivityId='" + getWorkflowActivityId() + "'" +
      ", status='" + getStatus() + "'" +
      ", initiatorContext='" + getInitiatorContext() + "'" +
      ", outputProperties='" + getOutputProperties() + "'" +
      ", errorResponse='" + getErrorResponse() + "'" +
      ", executedTasks='" + getExecutedTasks() + "'" +
      "}";
  }
  // @formatter:on
}
