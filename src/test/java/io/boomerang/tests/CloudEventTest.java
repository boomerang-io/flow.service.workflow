package io.boomerang.tests;

import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import io.boomerang.model.eventing.Event;
import io.boomerang.model.eventing.EventCancel;
import io.boomerang.model.eventing.EventFactory;
import io.boomerang.model.eventing.EventTrigger;
import io.boomerang.model.eventing.EventWFE;
import io.boomerang.mongo.model.TaskStatus;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

@ExtendWith(SpringExtension.class)
public class CloudEventTest {

  @Test
  public void testTriggerCloudEventWithoutProperties() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/topic/foobar\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    EventTrigger eventTrigger =
        (EventTrigger) Assertions.assertDoesNotThrow(() -> EventTrigger.fromCloudEvent(cloudEvent));
    String[] subjectComponents = cloudEvent.getSubject().substring(1).split("\\/");

    Assertions.assertEquals(eventTrigger.getId(), cloudEvent.getId());
    Assertions.assertEquals(eventTrigger.getSource(), cloudEvent.getSource());
    Assertions.assertEquals(eventTrigger.getSubject(), cloudEvent.getSubject());
    Assertions.assertEquals(eventTrigger.getWorkflowId(), subjectComponents[1]);
    Assertions.assertEquals(eventTrigger.getTopic(), subjectComponents[3]);
    Assertions.assertEquals(eventTrigger.getToken(), cloudEvent.getExtension("token"));
    Assertions.assertEquals(eventTrigger.getDate(),
        new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    Assertions.assertEquals(0, eventTrigger.getProperties().size());
  }

  @Test
  public void testTriggerCloudEventWithProperties() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/topic/foobar\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        ",\"data\":{",
          "\"textMessage\":\"It did go through!\"",
          ",\"name\":\"Iulian\"",
          "}",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    EventTrigger eventTrigger =
        (EventTrigger) Assertions.assertDoesNotThrow(() -> EventTrigger.fromCloudEvent(cloudEvent));
    String[] subjectComponents = cloudEvent.getSubject().substring(1).split("\\/");

    Assertions.assertEquals(eventTrigger.getId(), cloudEvent.getId());
    Assertions.assertEquals(eventTrigger.getSource(), cloudEvent.getSource());
    Assertions.assertEquals(eventTrigger.getSubject(), cloudEvent.getSubject());
    Assertions.assertEquals(eventTrigger.getWorkflowId(), subjectComponents[1]);
    Assertions.assertEquals(eventTrigger.getTopic(), subjectComponents[3]);
    Assertions.assertEquals(eventTrigger.getToken(), cloudEvent.getExtension("token"));
    Assertions.assertEquals(eventTrigger.getDate(),
        new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    Assertions.assertEquals(2, eventTrigger.getProperties().size());
  }

  @ParameterizedTest
  @ValueSource(strings = {"{\"just_a_string\":\"It did go through!\",\"just_a_num\":69420}",
      "[0,1,2,3,4,5,6]", "42069", "false", "[\"this\",false,202]"})
  public void testTriggerCloudEventInitiatorAndContext(String jsonContextField) {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/topic/foobar\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"initiatorid\":\"iulian\"",
        ",\"initiatorcontext\":" + jsonContextField,
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    EventTrigger eventTrigger =
        (EventTrigger) Assertions.assertDoesNotThrow(() -> EventTrigger.fromCloudEvent(cloudEvent));
    String[] subjectComponents = cloudEvent.getSubject().substring(1).split("\\/");

    Assertions.assertEquals(eventTrigger.getId(), cloudEvent.getId());
    Assertions.assertEquals(eventTrigger.getSource(), cloudEvent.getSource());
    Assertions.assertEquals(eventTrigger.getSubject(), cloudEvent.getSubject());
    Assertions.assertEquals(eventTrigger.getWorkflowId(), subjectComponents[1]);
    Assertions.assertEquals(eventTrigger.getTopic(), subjectComponents[3]);
    Assertions.assertEquals(eventTrigger.getToken(), cloudEvent.getExtension("token"));
    Assertions.assertEquals(eventTrigger.getDate(),
        new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    Assertions.assertEquals("iulian", eventTrigger.getInitiatorId());
    Assertions.assertEquals(0, eventTrigger.getProperties().size());
    Assertions.assertNotNull(eventTrigger.getInitiatorContext());
    Assertions.assertEquals(eventTrigger.getInitiatorContext(), jsonContextField);
  }

  @Test
  public void testTriggerCloudEventError1() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/topic/foobar\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Assertions.assertThrows(InvalidPropertiesFormatException.class,
        () -> EventTrigger.fromCloudEvent(cloudEvent));
  }

  @Test
  public void testTriggerCloudEventError2() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.wfe\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/topic/foobar\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Assertions.assertThrows(InvalidPropertiesFormatException.class,
        () -> EventTrigger.fromCloudEvent(cloudEvent));
  }

  @Test
  public void testWfeCloudEvent() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.wfe\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"status\":\"failure\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25/topic/foobar\"",
        ",\"time\":\"2022-05-06T12:45:15Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    EventWFE eventWFE =
        (EventWFE) Assertions.assertDoesNotThrow(() -> EventWFE.fromCloudEvent(cloudEvent));
    String[] subjectComponents = cloudEvent.getSubject().substring(1).split("\\/");

    Assertions.assertEquals(eventWFE.getId(), cloudEvent.getId());
    Assertions.assertEquals(eventWFE.getSource(), cloudEvent.getSource());
    Assertions.assertEquals(eventWFE.getSubject(), cloudEvent.getSubject());
    Assertions.assertEquals(eventWFE.getWorkflowId(), subjectComponents[1]);
    Assertions.assertEquals(eventWFE.getWorkflowActivityId(), subjectComponents[3]);
    Assertions.assertEquals(eventWFE.getTopic(), subjectComponents[5]);
    Assertions.assertEquals(eventWFE.getDate(),
        new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
    Assertions.assertEquals(TaskStatus.failure, eventWFE.getStatus());
  }

  @Test
  public void testWfeCloudEventError1() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.wfe\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"status\":\"failure\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/WHAT/cb4007aaf8b79b41ad598e25/topic/foobar\"",
        ",\"time\":\"2022-05-06T12:45:15Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Assertions.assertThrows(InvalidPropertiesFormatException.class,
        () -> EventWFE.fromCloudEvent(cloudEvent));
  }

  @Test
  public void testWfeCloudEventError2() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.cancel\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"status\":\"failure\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25/topic/foobar\"",
        ",\"time\":\"2022-05-06T12:45:15Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Assertions.assertThrows(InvalidPropertiesFormatException.class,
        () -> EventWFE.fromCloudEvent(cloudEvent));
  }

  @Test
  public void testCancelCloudEvent() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.cancel\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    EventCancel eventCancel =
        (EventCancel) Assertions.assertDoesNotThrow(() -> EventCancel.fromCloudEvent(cloudEvent));
    String[] subjectComponents = cloudEvent.getSubject().substring(1).split("\\/");

    Assertions.assertEquals(eventCancel.getId(), cloudEvent.getId());
    Assertions.assertEquals(eventCancel.getSource(), cloudEvent.getSource());
    Assertions.assertEquals(eventCancel.getSubject(), cloudEvent.getSubject());
    Assertions.assertEquals(eventCancel.getWorkflowId(), subjectComponents[1]);
    Assertions.assertEquals(eventCancel.getWorkflowActivityId(), subjectComponents[3]);
    Assertions.assertEquals(eventCancel.getToken(), cloudEvent.getExtension("token"));
    Assertions.assertEquals(eventCancel.getDate(),
        new Date(cloudEvent.getTime().toInstant().toEpochMilli()));
  }

  @Test
  public void testCancelCloudEventError1() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.cancel\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Assertions.assertThrows(InvalidPropertiesFormatException.class,
        () -> EventCancel.fromCloudEvent(cloudEvent));
  }

  @Test
  public void testCancelCloudEventError2() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25\"",
        ",\"token\":\"RXgGaXBzdW0gZG9sb3Ih\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Assertions.assertThrows(InvalidPropertiesFormatException.class,
        () -> EventCancel.fromCloudEvent(cloudEvent));
  }

  @Test
  public void testGenericEventForTriggerEvent() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.trigger\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/topic/foobar\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Event event = Assertions.assertDoesNotThrow(() -> EventFactory.buildFromCloudEvent(cloudEvent));
    Assertions.assertInstanceOf(EventTrigger.class, event);
  }

  @Test
  public void testGenericEventForWfeEvent() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.wfe\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"status\":\"success\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25/topic/foobar\"",
        ",\"time\":\"2022-05-06T12:45:15Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Event event = Assertions.assertDoesNotThrow(() -> EventFactory.buildFromCloudEvent(cloudEvent));
    Assertions.assertInstanceOf(EventWFE.class, event);
  }

  @Test
  public void testGenericEventForCancelEvent() {

    // @formatter:off
    String cloudEventData = String.join("", "{",
        "\"id\":\"36965047-1191-4aff-8e17-fe4e8c8e528a\"",
        ",\"type\":\"io.boomerang.event.workflow.cancel\"",
        ",\"source\":\"http://wdc2.cloud.boomerangplatform.net/listener/event\"",
        ",\"specversion\":\"1.0\"",
        ",\"datacontenttype\":\"application/json\"",
        ",\"subject\":\"/workflow/5f7f8cf69a7d401d9e584c90/activity/cb4007aaf8b79b41ad598e25\"",
        ",\"time\":\"2022-04-30T11:33:22Z\"",
        "}");
    // @formatter:on

    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(cloudEventData.getBytes());

    Event event = Assertions.assertDoesNotThrow(() -> EventFactory.buildFromCloudEvent(cloudEvent));
    Assertions.assertInstanceOf(EventCancel.class, event);
  }
}
