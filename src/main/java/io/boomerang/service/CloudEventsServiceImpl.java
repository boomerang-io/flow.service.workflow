package io.boomerang.service;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.webjars.NotFoundException;
import io.boomerang.config.EventingProperties;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.eventing.Event;
import io.boomerang.model.eventing.EventCancel;
import io.boomerang.model.eventing.EventFactory;
import io.boomerang.model.eventing.EventTrigger;
import io.boomerang.model.eventing.EventWFE;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.TaskService;
import io.cloudevents.CloudEvent;

@Service
public class CloudEventsServiceImpl implements CloudEventsService {

  private static final Logger logger = LogManager.getLogger(CloudEventsServiceImpl.class);

  protected static final String LABEL_KEY_EVENT_ID = "eventId";

  protected static final String LABEL_KEY_INITIATOR_ID = "initiatorId";

  protected static final String LABEL_KEY_INITIATOR_CONTEXT = "initiatorContext";

  @Autowired
  EventingProperties properties;

  @Lazy
  @Autowired
  private WorkflowService workflowService;

  @Lazy
  @Autowired
  private ExecutionService executionService;

  @Lazy
  @Autowired
  private FlowActivityService flowActivityService;

  @Lazy
  @Autowired
  private TaskService taskService;

  @Override
  public void processCloudEventRequest(CloudEvent cloudEvent)
      throws InvalidPropertiesFormatException {

    if (logger.isDebugEnabled()) {
      logger.debug("Extensions: {}", String.join(", ", cloudEvent.getExtensionNames()));
      logger.debug("Attributes: {}", String.join(", ", cloudEvent.getAttributeNames()));
      logger.debug("Data: {}", Optional.ofNullable(cloudEvent.getData()));
    }

    // Get the event
    Event event = EventFactory.buildFromCloudEvent(cloudEvent);
    String workflowId = getWorkflowIdFromEvent(event);
    WorkflowEntity workflowEntity = workflowService.getWorkflow(workflowId);

    // Check the workflow exists
    if (workflowEntity == null) {
      throw new NotFoundException(
          MessageFormat.format("Workflow with ID {0} not found!", workflowId));
    }

    // Check the custom events are activated
    if (!isCustomEventEnabled(event, workflowEntity)) {
      throw new BoomerangException(BoomerangError.WORKFLOW_TRIGGER_DISABLED);
    }

    // Check token is valid
    if (!isTokenValid(event, workflowEntity)) {
      throw new BoomerangException(BoomerangError.WORKFLOW_TOKEN_INVALID);
    }

    // Process the event
    logger.info("Type: {}", event.getType());

    switch (event.getType()) {
      case TRIGGER:

        EventTrigger eventTrigger = (EventTrigger) event;
        String eventProperties = eventTrigger.getProperties().keySet().stream()
            .map(key -> key + ": " + eventTrigger.getProperties().get(key))
            .collect(Collectors.joining(", "));

        if (logger.isDebugEnabled()) {
          logger.debug("WorkflowId: {}", eventTrigger.getWorkflowId());
          logger.debug("Topic: {}", eventTrigger.getTopic());
          logger.debug("InitiatorId: {}", eventTrigger.getInitiatorId());
          logger.debug("InitiatorContext: {}", eventTrigger.getInitiatorContext());
          logger.debug("Properties: {}", eventProperties);
        }

        // Create flow execution request
        FlowExecutionRequest executionRequest = new FlowExecutionRequest();
        executionRequest.setLabels(processEventLabels(eventTrigger));
        executionRequest
            .setProperties(processProperties(eventTrigger.getProperties(), workflowEntity));

        // Execute the workflow
        // @formatter:off
        executionService.executeWorkflow(
            eventTrigger.getWorkflowId(),
            Optional.of("Custom Event"),
            Optional.of(executionRequest),
            Optional.empty());
        // @formatter:on
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
      default:
        break;
    }
  }

  /**
   * Loop through a Workflow's parameters and if a JsonPath is set, read the event payload and
   * attempt to find parameters.
   */
  private Map<String, String> processProperties(Map<String, String> eventProperties,
      WorkflowEntity workflowEntity) {
    List<WorkflowProperty> inputProperties = workflowEntity.getProperties();
    Map<String, String> processedProperties = new HashMap<>();

    if (inputProperties != null) {

      for (WorkflowProperty inputProperty : inputProperties) {
        try {
          if (eventProperties != null && Strings.isNotEmpty(inputProperty.getJsonPath())) {

            String propertyValue = eventProperties
                .getOrDefault(inputProperty.getJsonPath(), inputProperty.getDefaultValue())
                .replaceAll("(^\\s*\"+)|(\"+\\s*$)", "");

            logger.info("processProperties() - Property: {}, Json Path: {}, Value: {}",
                inputProperty.getKey(), inputProperty.getJsonPath(), propertyValue);

            processedProperties.put(inputProperty.getKey(), propertyValue);
          }
        } catch (Exception e) {

          // Log and drop exception. We want the workflow to continue execution.
          logger.error(e);
        }
      }
    }

    String serializedEventProperties = "{}";

    if (eventProperties != null) {
      serializedEventProperties = eventProperties.keySet().stream()
          .map(key -> "\"" + key + "\":" + eventProperties.get(key))
          .collect(Collectors.joining(",", "{", "}"));
    }

    processedProperties.put("eventPayload", serializedEventProperties);
    processedProperties.forEach((k, v) -> logger.info("processProperties() - {}={}", k, v));

    return processedProperties;
  }

  private List<KeyValuePair> processEventLabels(EventTrigger et) {
    // Set cloud event labels
    String sharedLabelPrefix = properties.getShared().getLabel().getPrefix() + "/";
    List<KeyValuePair> cloudEventLabels = new LinkedList<>();
    cloudEventLabels.add(new KeyValuePair(sharedLabelPrefix + LABEL_KEY_EVENT_ID, et.getId()));

    if (Strings.isNotEmpty(et.getInitiatorId())) {
      cloudEventLabels
          .add(new KeyValuePair(sharedLabelPrefix + LABEL_KEY_INITIATOR_ID, et.getInitiatorId()));
    }

    if (et.getInitiatorContext() != null) {
      cloudEventLabels.add(new KeyValuePair(sharedLabelPrefix + LABEL_KEY_INITIATOR_CONTEXT,
          et.getInitiatorContext()));
    }
    return cloudEventLabels;
  }

  private String getWorkflowIdFromEvent(Event event) {
    switch (event.getType()) {
      case TRIGGER:
        return ((EventTrigger) event).getWorkflowId();
      case WFE:
        return ((EventWFE) event).getWorkflowId();
      case CANCEL:
        return ((EventCancel) event).getWorkflowId();
      default:
        return null;
    }
  }

  private boolean isCustomEventEnabled(Event event, WorkflowEntity workflowEntity) {

    try {
      Triggers triggers = workflowEntity.getTriggers();

      switch (event.getType()) {
        case TRIGGER:
          String topic = ((EventTrigger) event).getTopic();
          return triggers.getCustom().getEnable() && triggers.getCustom().getTopic().equals(topic);
        case WFE:
        case CANCEL:
          return triggers.getCustom().getEnable();
        default:
          return false;
      }
    } catch (Exception e) {
      logger.error(e);
    }

    return false;
  }

  private boolean isTokenValid(Event event, WorkflowEntity workflowEntity) {

    // Sanity check
    if (StringUtils.isBlank(event.getToken())) {
      return false;
    }

    return workflowEntity.getTokens().stream()
        .anyMatch(token -> token.getToken().equals(event.getToken()));
  }
}
