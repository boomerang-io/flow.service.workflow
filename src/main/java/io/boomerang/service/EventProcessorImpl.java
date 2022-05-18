package io.boomerang.service;

import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.config.EventingProperties;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.eventing.Event;
import io.boomerang.model.eventing.EventCancel;
import io.boomerang.model.eventing.EventFactory;
import io.boomerang.model.eventing.EventTrigger;
import io.boomerang.model.eventing.EventType;
import io.boomerang.model.eventing.EventWFE;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.TaskService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;

@Service
public class EventProcessorImpl implements EventProcessor {

  private static final Logger logger = LogManager.getLogger(EventProcessorImpl.class);

  @Autowired
  EventingProperties properties;

  @Autowired
  private FlowActivityService flowActivityService;

  @Autowired
  private WorkflowService workflowService;

  @Autowired
  private ExecutionService executionService;

  @Autowired
  private TaskService taskService;

  @Override
  public void processCloudEventRequest(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    logger.debug("processCloudEventRequest() - Extensions: {}",
        String.join(", ", cloudEvent.getExtensionNames()));
    logger.debug("processCloudEvent() - Attributes: {}",
        String.join(", ", cloudEvent.getAttributeNames()));
    logger.debug("processCloudEvent() - Data: {}", cloudEvent.getData().toString());

    // Get the event
    Event event = EventFactory.buildFromCloudEvent(cloudEvent);

    // Check the custom events are activated
    if (isCustomEventEnabled(event) == false) {
      // throw new NotAllowedException("");
      throw new BoomerangException(BoomerangError.WORKFLOW_TRIGGER_DISABLED);
    }

    // Process the event
    logger.info("processCloudEventRequest() - Type: {}", event.getType());

    switch (event.getType()) {
      case TRIGGER:
        String sharedLabelPrefix = properties.getShared().getLabel().getPrefix() + "/";
        EventTrigger eventTrigger = (EventTrigger) event;
        String eventProperties = eventTrigger.getProperties().keySet().stream()
            .map(key -> key + ": " + eventTrigger.getProperties().get(key))
            .collect(Collectors.joining(", "));

        logger.debug("processCloudEventRequest() - WorkflowId: {}", eventTrigger.getWorkflowId());
        logger.debug("processCloudEventRequest() - Topic: {}", eventTrigger.getTopic());
        logger.debug("processCloudEventRequest() - InitiatorId: {}", eventTrigger.getInitiatorId());
        logger.debug("processCloudEventRequest() - InitiatorContext: {}",
            eventTrigger.getInitiatorContext());
        logger.debug("processCloudEventRequest() - Properties: {}", eventProperties);

        // Set cloud event labels
        List<KeyValuePair> cloudEventLabels = new LinkedList<>();
        cloudEventLabels.add(new KeyValuePair(sharedLabelPrefix + "eventId", event.getId()));

        if (Strings.isNotEmpty(eventTrigger.getInitiatorId())) {
          cloudEventLabels.add(
              new KeyValuePair(sharedLabelPrefix + "initiatorid", eventTrigger.getInitiatorId()));
        }

        if (eventTrigger.getInitiatorContext().isEmpty() == false) {
          cloudEventLabels.add(new KeyValuePair(sharedLabelPrefix + "initiatorcontext",
              eventTrigger.getInitiatorContext().toString()));
        }

        // Create flow execution request
        FlowExecutionRequest executionRequest = new FlowExecutionRequest();
        executionRequest.setLabels(cloudEventLabels);
        executionRequest.setProperties(eventTrigger.getProperties());

        // Execute the workflow
        FlowActivity activity = executionService.executeWorkflow(eventTrigger.getWorkflowId(),
            Optional.of(event.getType().toString()), Optional.of(executionRequest),
            Optional.empty());
        break;

      case WFE:
        EventWFE eventWFE = (EventWFE) event;

        logger.debug("processCloudEventRequest() - WorkflowId: {}", eventWFE.getWorkflowId());
        logger.debug("processCloudEventRequest() - WorkflowActivityId: {}",
            eventWFE.getWorkflowId());
        logger.debug("processCloudEventRequest() - Topic: {}", eventWFE.getTopic());
        logger.debug("processCloudEventRequest() - Status: {}", eventWFE.getStatus());

        logger.info("processCloudEvent() - Wait For Event System Task");

        List<String> taskActivityId = taskService
            .updateTaskActivityForTopic(eventWFE.getWorkflowActivityId(), eventWFE.getTopic());

        for (String id : taskActivityId) {
          taskService.submitActivity(id, eventWFE.getStatus().toString(), Map.of());
        }
        break;

      case CANCEL:
        EventCancel eventCancel = (EventCancel) event;

        logger.debug("processCloudEventRequest() - WorkflowId: {}", eventCancel.getWorkflowId());
        logger.debug("processCloudEventRequest() - WorkflowActivityId: {}",
            eventCancel.getWorkflowId());

        flowActivityService.cancelWorkflowActivity(eventCancel.getWorkflowActivityId(), null);
        break;
    }
  }

  @Override
  public void processNATSMessage(String message) throws InvalidPropertiesFormatException {
    CloudEvent cloudEvent = EventFormatProvider.getInstance().resolveFormat(JsonFormat.CONTENT_TYPE)
        .deserialize(message.getBytes());
    processCloudEventRequest(cloudEvent);
  }

  private Boolean isCustomEventEnabled(Event event) {

    // @formatter:off
    Map<EventType, Callable<String>> getWorkflowIdByClass = Map.of(
      EventType.TRIGGER, ((EventTrigger) event)::getWorkflowId,
      EventType.WFE, ((EventWFE) event)::getWorkflowId,
      EventType.CANCEL, ((EventCancel) event)::getWorkflowId
    );
    // @formatter:on

    try {
      String workflowId = getWorkflowIdByClass.get(event.getType()).call();
      Triggers triggers = workflowService.getWorkflow(workflowId).getTriggers();

      switch (event.getType()) {
        case TRIGGER:
          String topic = ((EventTrigger) event).getTopic();
          return triggers.getCustom().getEnable() && triggers.getCustom().getTopic().equals(topic);
        case WFE:
        case CANCEL:
          return triggers.getCustom().getEnable();
      }
    } catch (Exception e) {
      logger.error(e);
    }

    return false;
  }
}
