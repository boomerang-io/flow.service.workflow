package net.boomerangplatform.service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.AttributesImpl;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import io.cloudevents.v1.http.Unmarshallers;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.model.eventing.EventResponse;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.model.Triggers;
import net.boomerangplatform.service.crud.WorkflowService;
import net.boomerangplatform.service.refactor.TaskService;

@Service
public class EventProcessorImpl implements EventProcessor {

  protected static final String TYPE_PREFIX = "io.boomerang.eventing.";

  private static final Logger logger = LogManager.getLogger(EventProcessorImpl.class);

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private ExecutionService executionService;

  @Autowired
  private TaskService taskService;
  
  // TODO: better return management
  @Override
  public CloudEventImpl<EventResponse> processHTTPEvent(Map<String, Object> headers,
      JsonNode payload) {
    CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
        .withHeaders(() -> headers).withPayload(() -> payload.toString()).unmarshal();

    return createResponseEvent(event.getAttributes().getId(), event.getAttributes().getType(),
        event.getAttributes().getSource(), event.getAttributes().getSubject().orElse(""),
        event.getAttributes().getTime().orElse(ZonedDateTime.now()), processEvent(event));
  }

  // TODO: better return management
  @Override
  public void processNATSMessage(String message) {

    logger.info("processNATSMessage() - Message: " + message);
    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", "application/cloudevents+json");

    CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
        .withHeaders(() -> headers).withPayload(() -> message).unmarshal();

    // TODO return message to another queue for picking up?
     createResponseEvent(event.getAttributes().getId(), event.getAttributes().getType(),
     event.getAttributes().getSource(), event.getAttributes().getSubject().orElse(""),
     event.getAttributes().getTime().orElse(ZonedDateTime.now()), processEvent(event));
  }

  private CloudEventImpl<EventResponse> createResponseEvent(String id, String type, URI source,
      String subject, ZonedDateTime time, EventResponse responseData) {
    final CloudEventImpl<EventResponse> response =
        CloudEventBuilder.<EventResponse>builder().withId(id).withType(type).withSource(source)
            .withData(responseData).withSubject(subject).withTime(time).build();

    return response;
  }

  private EventResponse processEvent(CloudEvent<AttributesImpl, JsonNode> event) {
    logger.info("processCloudEvent() - Attributes: " + event.getAttributes().toString());
    JsonNode eventData = event.getData().get();
    logger.info("processCloudEvent() - Data: " + eventData.toPrettyString());

    EventResponse response = new EventResponse();

    String subject = event.getAttributes().getSubject().orElse("");
    logger.info("processCloudEvent() - Subject: " + subject);
    if (!subject.startsWith("/")) {
      logger.error(
          "processCloudEvent() - Error: subject does not conform to required standard of /{workflowId} followed by /{topic} if custom event");
      response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
      response.setStatusMessage(
          "Event subject does not conform to required standard of /{workflowId} followed by /{topic} if custom event");
      return response;
    }

    String workflowId = getWorkflowIdFromSubject(subject);
    if (workflowId.isBlank()) {
      logger.error("processCloudEvent() - Error: unable to retrieve workflowId from Event subject");
      response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
      response.setStatusMessage("unable to retrieve workflowId from Event subject");
      return response;

    }
    logger.info("processCloudEvent() - WorkflowId: " + workflowId);

    String topic = getTopicFromSubject(subject);
    String trigger = event.getAttributes().getType().replace(TYPE_PREFIX, "");
    logger.info("processCloudEvent() - Trigger: " + trigger + ", Topic: " + topic);

    if (isTriggerEnabled(trigger, workflowId, topic)) {
      logger.info("processCloudEvent() - Trigger(" + trigger + ") is enabled");

      FlowExecutionRequest executionRequest = new FlowExecutionRequest();
      executionRequest.setProperties(processProperties(eventData, workflowId));

      FlowActivity activity = executionService.executeWorkflow(workflowId,
          Optional.of(trigger), Optional.of(executionRequest));
      response.setActivityId(activity.getId());
      response.setStatusCode(HttpStatus.SC_OK);
      return response;
    } else if ("wfe".equals(trigger)) {
      logger.info("processCloudEvent() - Wait For Event System Task");
      String workflowActivityId = getWorkflowActivityIdFromSubject(subject);
      List<String> taskActivityId = taskService.updateTaskActivityForTopic(workflowActivityId, topic);
      for (String id : taskActivityId) {
        taskService.submitActivity(id);
      }
      
    } else {
      // TODO make error
      logger.error("processCloudEvent() - No matching trigger enabled.");
      response.setStatusCode(HttpStatus.SC_FORBIDDEN);
      response.setStatusMessage("Event did not match enabled workflow trigger.");
      return response;
    }
    
    // TODO returning null to fix compilation errors
    return null;
  }

  private Map<String, String> processProperties(JsonNode eventData, String workflowId) {
    Configuration jacksonConfig =
        Configuration.builder().mappingProvider(new JacksonMappingProvider())
            .jsonProvider(new JacksonJsonNodeJsonProvider())
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL).build();
    List<FlowProperty> inputProperties = workflowService.getWorkflow(workflowId).getProperties();
    Map<String, String> properties = new HashMap<>();
    if (inputProperties != null) {
      try {
        inputProperties.forEach(inputProperty -> {
          // TODO change to not parse the document every time
          if (inputProperty.getJsonPath() != null && !inputProperty.getJsonPath().isBlank()) {
            JsonNode propertyValue =
                JsonPath.using(jacksonConfig).parse(eventData).read(inputProperty.getJsonPath());
  
            if (!propertyValue.isNull()) {
              logger.info("processProperties() - Property: " + inputProperty.getKey() + ", Json Path: " + inputProperty.getJsonPath()
                  + ", Value: " + propertyValue.toString());
              properties.put(inputProperty.getKey(), propertyValue.toString());
            } else {
              logger.info("processProperties() - Skipping property: " + inputProperty.getKey());
            }
          }
        });
      } catch (Exception e) {
        // TODO deal with exception
        logger.error(e.toString());
      }
    }

    properties.put("io.boomerang.eventing.data", eventData.toString());

    properties.forEach((k, v) -> {
      logger.info("processProperties() - " + k + "=" + v);
    });

    return properties;
  }

  private Boolean isTriggerEnabled(String trigger, String workflowId, String topic) {

    Triggers triggers = workflowService.getWorkflow(workflowId).getTriggers();

    switch (trigger) {
      case "manual":
        return triggers.getManual().getEnable();
      case "scheduler":
        return triggers.getScheduler().getEnable();
      case "webhook":
      case "dockerhub":
      case "slack":
        return triggers.getWebhook().getEnable();
      case "custom":
        if (triggers.getCustom().getEnable()) {
          return topic
              .equals(workflowService.getWorkflow(workflowId).getTriggers().getCustom().getTopic());
        } ;
    }
    return false;
  }

  private String getWorkflowIdFromSubject(String subject) {
    // Reference 0 will be an empty string as it is the left hand side of the split
    String[] splitArr = subject.split("/");
    if (splitArr.length >= 2) {
      return splitArr[1].toString();
    } else {
      return "";
    }
  }
  
  private String getWorkflowActivityIdFromSubject(String subject) {
    // Reference 0 will be an empty string as it is the left hand side of the split
    String[] splitArr = subject.split("/");
    if (splitArr.length >= 3) {
      return splitArr[2].toString();
    } else {
      return "";
    }
  }

  private String getTopicFromSubject(String subject) {
    // Reference 0 will be an empty string as it is the left hand side of the split
    String[] splitArr = subject.split("/");
    if (splitArr.length >= 4) {
      return splitArr[3].toString();
    } else if (splitArr.length >= 3) {
        return splitArr[2].toString();
    } else {
      return "";
    }
  }
}
