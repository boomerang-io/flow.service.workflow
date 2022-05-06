package io.boomerang.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.boomerang.model.eventing.EventTrigger;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

@ExtendWith(SpringExtension.class)
public class CloudEventTest {

  @Test
  public void testTriggerCloudEvent() throws Exception {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.eventing.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/5f7f8cf69a7d401d9e584c90/foobar\"",
        ",\"time\":\"2022-05-06T12:45:15Z\"",
        ",\"data\":{",
          "\"textMessage\":\"It did go through!\"",
          ",\"name\":\"Iulian\"",
          "}",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());
    Assertions.assertDoesNotThrow(() -> {
      EventTrigger.fromCloudEvent(cloudEvent);
    });
  }
}
