package net.boomerangplatform.misc;

import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import io.cloudevents.CloudEvent;
import io.cloudevents.json.Json;
import io.cloudevents.v1.AttributesImpl;
import io.cloudevents.v1.CloudEventBuilder;
import io.cloudevents.v1.CloudEventImpl;
import io.cloudevents.v1.http.Unmarshallers;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.tests.TestUtil;

public class EventProcessorTest {

  @Test
  public void testCloudEventProcessing() throws IOException {
//    Build Cloud Event
    String payload = buildEvent();
    
//    Unmarshall Cloud Event
    Map<String, Object> headers = new HashMap<>();
    headers.put("Content-Type", "application/cloudevents+json");

    System.out.println("Process Message - Event as Message String: " + payload);

    CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
        .withHeaders(() -> headers).withPayload(() -> payload).unmarshal();

    System.out.println("Process Message - Attributes: " + event.getAttributes().toString());
    System.out.println("Process Message - Payload: " + event.getData().get().toString());
    
    JsonNode cloudEventData = event.getData().get();
    if (cloudEventData != null) {     
      System.out.println("Process Message - data as JsonNode: " + cloudEventData);
    };
    
    processTrigger(cloudEventData.toString(), "1234", "dockerhub");
  }
  
  private String buildEvent() throws IOException {
    final String eventId = UUID.randomUUID().toString();
    final String eventType = "io.boomerang.eventing.test";
    final URI uri = URI.create("https://localhost/listener");
    String payloadAsString = TestUtil.getMockFile("json/event-dockerhub-payload.json");
    ObjectMapper mapper = new ObjectMapper();
    JsonNode payload = mapper.readTree(payloadAsString);
    
    System.out.println("Payload: " + payload.toPrettyString());
        
    final CloudEventImpl<JsonNode> cloudEvent =
    CloudEventBuilder.<JsonNode>builder()
      .withType(eventType)
      .withId(eventId)
      .withSource(uri)
      .withData(payload)
      .withSubject("test")
      .withTime(ZonedDateTime.now())
      .build();
    
    final String jsonPayload = Json.encode(cloudEvent);
    System.out.println("CloudEvent: " + jsonPayload);

    return jsonPayload;
  }

  private void processTrigger(String payload, String workflowId, String trigger) {
    Configuration jacksonConfig =
        Configuration.builder().mappingProvider(new JacksonMappingProvider())
            .jsonProvider(new JacksonJsonProvider()).build();
    List<FlowProperty> inputProperties = new LinkedList<>();
    FlowProperty flowProperty1 = new FlowProperty();
    flowProperty1.setKey("callback_url");
    inputProperties.add(flowProperty1);
    FlowProperty flowProperty2 = new FlowProperty();
    flowProperty2.setKey("push_data.images");
    inputProperties.add(flowProperty2);
    Map<String, String> properties = new HashMap<>();
    if (inputProperties != null) {
      assertNotNull(inputProperties);
      inputProperties.forEach(inputProperty -> {
        JsonNode propertyValue =
            JsonPath.using(jacksonConfig).parse(payload).read("$." + inputProperty.getKey(), JsonNode.class);

        if (propertyValue != null) {
          properties.put(inputProperty.getKey(), propertyValue.toString());
        }
        
        properties.forEach((k, v) -> {System.out.println(k + "=" + v);});
      });
      
//      assertEquals(properties.get("callback_url").toString(), "https://registry.hub.docker.com/u/svendowideit/testhook/hook/2141b5bi5i5b02bec211i4eeih0242eg11000a/");
    }
  }
}
