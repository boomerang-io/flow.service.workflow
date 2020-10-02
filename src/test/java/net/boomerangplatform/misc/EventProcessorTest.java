package net.boomerangplatform.misc;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import net.boomerangplatform.mongo.model.FlowProperty;
import net.boomerangplatform.tests.TestUtil;

public class EventProcessorTest {

  @Test
  public void testJsonPath() throws IOException {
    String payload = TestUtil.getMockFile("json/event-dockerhub-payload.json");

    processTrigger(payload, "1234", "dockerhub");
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
      inputProperties.forEach(inputProperty -> {
        JsonNode propertyValue =
            JsonPath.using(jacksonConfig).parse(payload).read("$." + inputProperty.getKey(), JsonNode.class);

        if (propertyValue != null) {
          properties.put(inputProperty.getKey(), propertyValue.toString());
        }
        
        properties.forEach((k, v) -> {System.out.println(k + "=" + v);});
      });
    }
  }
}
