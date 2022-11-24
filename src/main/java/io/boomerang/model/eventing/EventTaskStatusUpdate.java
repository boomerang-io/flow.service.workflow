package io.boomerang.model.eventing;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.util.Strings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import io.boomerang.mongo.model.ErrorResponse;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.TaskStatus;
import io.boomerang.mongo.model.TaskType;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

public class EventTaskStatusUpdate extends Event {

  private String taskName;

  private String taskId;

  private String workflowId;

  private String workflowActivityId;

  private TaskStatus status;

  private TaskType taskType;

  private String initiatorContext;

  private List<KeyValuePair> outputProperties;

  private ErrorResponse errorResponse;

  private Map<String, String> additionalData;

  @Override
  public CloudEvent toCloudEvent() throws IOException {

    JsonObject jsonData = new JsonObject();
    jsonData.addProperty("taskname", taskName);
    jsonData.addProperty("taskid", taskId);
    jsonData.addProperty("workflowid", workflowId);
    jsonData.addProperty("workflowactivityid", workflowActivityId);
    jsonData.addProperty("status", status.toString());
    jsonData.addProperty("tasktype", taskType.toString());

    // Configure and create Gson object
    Gson gson = new GsonBuilder().create();

    // Add output properties to JSON data
    if (outputProperties != null && !outputProperties.isEmpty()) {
      jsonData.add("outputProperties", gson.toJsonTree(outputProperties));
    }

    // Add error data to JSON data
    if (errorResponse != null) {
      jsonData.add("error", gson.toJsonTree(errorResponse));
    }

    // Add additional data to JSON data
    if (additionalData != null && !additionalData.isEmpty()) {
      additionalData.entrySet()
          .forEach(entry -> jsonData.addProperty(entry.getKey(), entry.getValue()));
    }

    // @formatter:off
    CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v03()
        .withId(getId())
        .withSource(getSource())
        .withSubject(getSubject())
        .withType(getType().getCloudEventType())
        .withTime(getDate().toInstant().atOffset(ZoneOffset.UTC))
        .withData(ContentType.APPLICATION_JSON.toString(), jsonData.toString().getBytes());
    // @formatter:on

    if (Strings.isNotEmpty(initiatorContext)) {
      cloudEventBuilder =
          cloudEventBuilder.withExtension(EXTENSION_ATTRIBUTE_CONTEXT, initiatorContext);
    }

    return cloudEventBuilder.build();
  }

  public String getTaskName() {
    return this.taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  public String getTaskId() {
    return this.taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
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

  public TaskType getTaskType() {
    return this.taskType;
  }

  public void setTaskType(TaskType taskType) {
    this.taskType = taskType;
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

  public Map<String, String> getAdditionalData() {
    return this.additionalData;
  }

  public void setAdditionalData(Map<String, String> additionalData) {
    this.additionalData = additionalData;
  }

  // @formatter:off
  @Override
  public String toString() {
    return "{" +
      " taskName='" + getTaskName() + "'" +
      ", taskId='" + getTaskId() + "'" +
      ", workflowId='" + getWorkflowId() + "'" +
      ", workflowActivityId='" + getWorkflowActivityId() + "'" +
      ", status='" + getStatus() + "'" +
      ", taskType='" + getTaskType() + "'" +
      ", initiatorContext='" + getInitiatorContext() + "'" +
      ", outputProperties='" + getOutputProperties() + "'" +
      ", errorResponse='" + getErrorResponse() + "'" +
      ", additionalData='" + getAdditionalData() + "'" +
      "}";
  }
  // @formatter:on
}
