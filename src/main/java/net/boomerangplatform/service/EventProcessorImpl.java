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
  public void processEvent(Map<String, Object> headers, JsonNode payload) {

    CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
        .withHeaders(() -> headers).withPayload(() -> payload.toString()).unmarshal();

    logger.info("Process Message - Attributes: " + event.getAttributes().toString());
    JsonNode eventData = event.getData().get();
    logger.info("Process Message - Data: " + eventData.toPrettyString());

    String workflowId = event.getAttributes().getSubject().orElse("");
    String trigger = event.getAttributes().getType().replace(TYPE_PREFIX, "");

    processTrigger(eventData, workflowId, trigger);
  }

  // TODO: better return management
  @Override
  public void processMessage(String message) {
    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", "application/cloudevents+json");

    logger.info("Process Message - Event as Message String: " + message);

    CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
        .withHeaders(() -> headers).withPayload(() -> message).unmarshal();

    logger.info("Process Message - Attributes: " + event.getAttributes().toString());
    JsonNode eventData = event.getData().get();
    logger.info("Process Message - Data: " + eventData.toPrettyString());

    String workflowId = "";
    String topic = "";
    String subject = event.getAttributes().getSubject().orElse("");
    String[] splitArr = subject.split("/");
    for (Integer i=0; i < splitArr.length; i++) {
      System.out.println(splitArr[i]);
    }
    workflowId = splitArr[0].toString();
    topic = splitArr[1].toString();
    logger.info("Process Message - WorkflowId: " + workflowId + ", Topic: " + topic);
    String trigger = event.getAttributes().getType().replace(TYPE_PREFIX, "");

    processTrigger(eventData, workflowId, trigger);
  }

  private void processTrigger(JsonNode eventData, String workflowId, String trigger) {

    if (isTriggerEnabled(trigger, workflowId)) {
      logger.info("Process Message - Trigger(" + trigger + ") is allowed.");
      Configuration jacksonConfig =
          Configuration.builder().mappingProvider(new JacksonMappingProvider())
              .jsonProvider(new JacksonJsonProvider()).build();
      List<FlowProperty> inputProperties = workflowService.getWorkflow(workflowId).getProperties();
      Map<String, String> properties = new HashMap<>();
      if (inputProperties != null) {
        inputProperties.forEach(inputProperty -> {
          logger.info("Process Message - Property Key: " + inputProperty.getKey());
          JsonNode propertyValue = JsonPath.using(jacksonConfig).parse(eventData.toString())
              .read("$." + inputProperty.getKey(), JsonNode.class);
          logger.info("Process Message - Property Value: " + propertyValue.toString());

          if (propertyValue != null) {
            properties.put(inputProperty.getKey(), propertyValue.toString());
          }

          properties.forEach((k, v) -> {
            logger.info("Process Message - " + k + "=" + v);
          });
        });
      }

      FlowExecutionRequest executionRequest = new FlowExecutionRequest();
      executionRequest.setProperties(properties);

      executionService.executeWorkflow(workflowId, Optional.empty(), Optional.of(executionRequest));
    }
  }


  private Boolean isTriggerEnabled(String trigger, String workflowId) {

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
        return triggers.getSlack().getEnable();
//      default:
//        if (triggers.getCustom().getEnable()) {
//          return trigger
//              .equals(workflowService.getWorkflow(workflowId).getTriggers().getCustom().getTopic());
//        } ;
    }
    return false;
  }
}
