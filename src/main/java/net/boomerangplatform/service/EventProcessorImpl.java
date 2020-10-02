package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.AttributesImpl;
import io.cloudevents.v1.http.Unmarshallers;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.model.Triggers;
import net.boomerangplatform.service.crud.WorkflowService;

@Service
public class EventProcessorImpl implements EventProcessor {

  protected static final String TYPE_PREFIX = "io.boomerang.eventing.";

  private static final Logger logger = LogManager.getLogger(EventProcessorImpl.class);

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private ExecutionService executionService;


  // TODO: better return management
  @Override
  public void processHTTPEvent(Map<String, Object> headers, JsonNode payload) {
    CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
        .withHeaders(() -> headers).withPayload(() -> payload.toString()).unmarshal();
    
    processCloudEvent(event);
  }

  // TODO: better return management
  @Override
  public void processJSONMessage(String message) {
    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", "application/cloudevents+json");

    CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
        .withHeaders(() -> headers).withPayload(() -> message).unmarshal();
    
    processCloudEvent(event);
  }
  
  private void processCloudEvent(CloudEvent<AttributesImpl, JsonNode> event) {
    logger.info("processCloudEvent() - Attributes: " + event.getAttributes().toString());
    JsonNode eventData = event.getData().get();
    logger.info("processCloudEvent() - Data: " + eventData.toPrettyString());

    String workflowId = "";
    String topic = "";
    String subject = event.getAttributes().getSubject().orElse("");
    logger.info("processCloudEvent() - Subject: " + subject);
    if (!subject.startsWith("/")) {
//      TODO make error
      logger.error("processCloudEvent() - Error: subject does not conform to required standard of /{workflowId} followed by /{topic} if custom event");
    }
    // Reference 0 will be an empty string as it is the left hand side of the split
    String[] splitArr = subject.split("/");
    if (splitArr.length > 1) {
      workflowId = splitArr[1].toString();
    } else {
      logger.error("processCloudEvent() - Error: No workflow ID found in event");
    }
    logger.info("processCloudEvent() - WorkflowId: " + workflowId);
    if (splitArr.length > 2) {
      topic = splitArr[2].toString();
    }
    String trigger = event.getAttributes().getType().replace(TYPE_PREFIX, "");
    logger.info("processCloudEvent() - Trigger: " + trigger + "Topic: " + topic);

    if (isTriggerEnabled(trigger, workflowId, topic)) {
      logger.info("processCloudEvent() - Trigger(" + trigger + ") is allowed.");

      FlowExecutionRequest executionRequest = new FlowExecutionRequest();
      executionRequest.setProperties(processProperties(eventData, workflowId));

      executionService.executeWorkflow(workflowId, Optional.of(FlowTriggerEnum.getFlowTriggerEnum(trigger)), Optional.of(executionRequest));
    } else {
//    TODO make error
      logger.error("processCloudEvent() - No matching trigger enabled.");
    }
  }
  
  private Map<String, String> processProperties(JsonNode eventData, String workflowId) {
    Configuration jacksonConfig =
        Configuration.builder().mappingProvider(new JacksonMappingProvider())
            .jsonProvider(new JacksonJsonProvider()).build();
    List<FlowProperty> inputProperties = workflowService.getWorkflow(workflowId).getProperties();
    Map<String, String> properties = new HashMap<>();
    if (inputProperties != null) {
      inputProperties.forEach(inputProperty -> {
        logger.info("processProperties() - Property Key: " + inputProperty.getKey());
        JsonNode propertyValue = JsonPath.using(jacksonConfig).parse(eventData.toString())
            .read("$." + inputProperty.getKey(), JsonNode.class);
        logger.info("processProperties() - Property Value: " + propertyValue.toString());

        if (propertyValue != null) {
          properties.put(inputProperty.getKey(), propertyValue.toString());
        }

        properties.forEach((k, v) -> {
          logger.info("processProperties() - " + k + "=" + v);
        });
      });
    }
    
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
        return triggers.getWebhook().getEnable();
      case "dockerhub":
        return triggers.getDockerhub().getEnable();
      case "slack":
        return triggers.getSlack().getEnable();
      case "custom":
        if (triggers.getCustom().getEnable()) {
          return topic
              .equals(workflowService.getWorkflow(workflowId).getTriggers().getCustom().getTopic());
        } ;
    }
    return false;
  }
}
