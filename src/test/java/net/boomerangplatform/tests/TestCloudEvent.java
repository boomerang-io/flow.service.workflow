package net.boomerangplatform.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.v1.AttributesImpl;
import io.cloudevents.v1.http.Unmarshallers;

@SpringBootTest
public class TestCloudEvent {

  @Test
  public void testCloudEvent() {
    Map<String, Object> headers = new HashMap<>();
    String natsMessage = "{\"data\":{\"name\":\"tyson\"},\"id\":\"f182708f-025d-4fce-ba32-7686bab23a69\",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/webhook/wfe\",\"specversion\":\"1.0\",\"type\":\"io.boomerang.eventing.wfe\",\"subject\":\"/5f7f8cf69a7d401d9e584c90/600f7e9ea8a2c75dc5d8deab/bar\",\"time\":\"2021-01-26T02:30:00.968526889Z\",\"status\":\"failure\"}";
    JsonNode payload;
    try {
      payload = getJsonNode(natsMessage);
      CloudEvent<AttributesImpl, JsonNode> event = Unmarshallers.structured(JsonNode.class)
          .withHeaders(() -> headers).withPayload(() -> payload.toString()).unmarshal();
      System.out.println(event.getAttributes().getTime().get());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private JsonNode getJsonNode(String jsonString)
      throws IOException, JsonParseException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    JsonFactory factory = mapper.getFactory();
    JsonParser parser = factory.createParser(jsonString);
    JsonNode actualObj = mapper.readTree(parser);
    return actualObj;
  }

}
