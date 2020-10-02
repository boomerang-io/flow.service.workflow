package net.boomerangplatform.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.AttributesImpl;
import io.cloudevents.v1.http.Unmarshallers;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.model.FlowProperty;
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

    CloudEvent<AttributesImpl, Map> event = Unmarshallers.structured(Map.class)
        .withHeaders(() -> headers).withPayload(() -> payload.toString()).unmarshal();

    logger.info("Process Message - Attributes: " + event.getAttributes().toString());
    logger.info("Process Message - Payload: " + event.getData().toString());

    processTrigger(payload, event);
  }

  // TODO: better return management
  @Override
  public void processMessage(String payload) {
    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", "application/cloudevents+json");

    CloudEvent<AttributesImpl, Map> event = Unmarshallers.structured(Map.class)
        .withHeaders(() -> headers).withPayload(() -> payload).unmarshal();

    logger.info("Process Message - Attributes: " + event.getAttributes().toString());
    logger.info("Process Message - Payload: " + event.getData().toString());

    ObjectMapper mapper = new ObjectMapper();

    try {
      processTrigger(mapper.readTree(payload), event);
    } catch (JsonMappingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (JsonProcessingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private void processTrigger(JsonNode payload, CloudEvent<AttributesImpl, Map> event) {
    // TODO determine the trigger implementation
    String workflowId = event.getAttributes().getSubject().orElse("");
    String trigger = event.getAttributes().getType().replace(TYPE_PREFIX, "");


    if (trigger
        .equals(workflowService.getWorkflow(workflowId).getTriggers().getEvent().getTopic())) {
      logger.info("Process Message - Trigger(" + trigger + ") is allowed.");

      Configuration jacksonConfig = Configuration.builder()
          .mappingProvider( new JacksonMappingProvider() )
          .jsonProvider( new JacksonJsonProvider() )
          .build();
      List<FlowProperty> inputProperties = workflowService.getWorkflow(workflowId).getProperties();
      Map<String, String> properties = new HashMap<>();
      if (inputProperties != null) {
        inputProperties.forEach(inputProperty -> {
          String propertyKey = "$." + inputProperty.getKey();
          logger.info("Process Message - Property Key: " + propertyKey);
          JsonNode propertyValue = JsonPath.using(jacksonConfig).parse(payload.toString()).read(propertyKey, JsonNode.class);
          logger.info("Process Message - Property Value: " + propertyValue.toPrettyString());

          if (propertyValue != null) {
            properties.put(propertyKey, propertyValue.toString());
          }
        });
      }

      FlowExecutionRequest executionRequest = new FlowExecutionRequest();
      executionRequest.setProperties(properties);

      executionService.executeWorkflow(workflowId, Optional.empty(), Optional.of(executionRequest));
    }
  }
}
