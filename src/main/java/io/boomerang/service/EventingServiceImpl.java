package io.boomerang.service;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import io.boomerang.config.EventingProperties;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.eventing.nats.ConnectionPrimer;
import io.boomerang.eventing.nats.jetstream.PubOnlyConfiguration;
import io.boomerang.eventing.nats.jetstream.PubOnlyTunnel;
import io.boomerang.eventing.nats.jetstream.PubSubConfiguration;
import io.boomerang.eventing.nats.jetstream.PubSubTransceiver;
import io.boomerang.eventing.nats.jetstream.PubTransmitter;
import io.boomerang.eventing.nats.jetstream.SubHandler;
import io.boomerang.eventing.nats.jetstream.SubOnlyTunnel;
import io.boomerang.eventing.nats.jetstream.exception.StreamNotFoundException;
import io.boomerang.eventing.nats.jetstream.exception.SubjectMismatchException;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.eventing.Event;
import io.boomerang.model.eventing.EventCancel;
import io.boomerang.model.eventing.EventFactory;
import io.boomerang.model.eventing.EventStatusUpdate;
import io.boomerang.model.eventing.EventTrigger;
import io.boomerang.model.eventing.EventWFE;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.mongo.model.WorkflowProperty;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.WorkflowService;
import io.boomerang.service.refactor.TaskService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.nats.client.JetStreamApiException;
import io.nats.client.Options;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StreamConfiguration;

@Service
public class EventingServiceImpl implements EventingService, SubHandler {

  private static final Logger logger = LogManager.getLogger(EventingServiceImpl.class);

  protected static final String LABEL_KEY_EVENT_ID = "eventId";

  protected static final String LABEL_KEY_INITIATOR_ID = "initiatorId";

  protected static final String LABEL_KEY_INITIATOR_CONTEXT = "initiatorContext";

  @Autowired
  EventingProperties properties;

  @Lazy
  @Autowired
  private FlowActivityService flowActivityService;

  @Lazy
  @Autowired
  private WorkflowService workflowService;

  @Lazy
  @Autowired
  private ExecutionService executionService;

  @Lazy
  @Autowired
  private TaskService taskService;

  private ConnectionPrimer connectionPrimer;

  private PubOnlyTunnel statusEventsTunnel;

  private SubOnlyTunnel actionEventsTunnel;

  private EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

  @PostConstruct
  private void init() {

    // Build connection primer
    // @formatter:off
    Options.Builder optionsBuilder = new Options.Builder()
        .servers(properties.getNats().getServer().getUrls())
        .reconnectWait(properties.getNats().getServer().getReconnectWaitTime())
        .maxReconnects(properties.getNats().getServer().getReconnectMaxAttempts());
    // @formatter:on
    connectionPrimer = new ConnectionPrimer(optionsBuilder);

    // Create the tunnel for action CloudEvents (trigger, wait for event, cancel, etc.)
    // @formatter:off
    StreamConfiguration streamConfiguration = new StreamConfiguration.Builder()
        .name(properties.getJetstream().getActionEvents().getStream().getName())
        .storageType(properties.getJetstream().getActionEvents().getStream().getStorageType())
        .subjects(properties.getJetstream().getActionEvents().getStream().getSubjects())
        .build();
    ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration.Builder()
        .durable(properties.getJetstream().getActionEvents().getConsumer().getName())
        .build();
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration.Builder()
        .automaticallyCreateStream(true)
        .automaticallyCreateConsumer(true)
        .build();
    // @formatter:on
    actionEventsTunnel = new PubSubTransceiver(connectionPrimer, streamConfiguration,
        consumerConfiguration, pubSubConfiguration);

    // Create the tunnel for status update CloudEvents
    // @formatter:off
    StreamConfiguration statusStreamConfiguration = new StreamConfiguration.Builder()
        .name(properties.getJetstream().getStatusEvents().getStream().getName())
        .storageType(properties.getJetstream().getStatusEvents().getStream().getStorageType())
        .subjects(properties.getJetstream().getStatusEvents().getStream().getSubjects())
        .build();
    PubOnlyConfiguration pubOnlyConfiguration = new PubOnlyConfiguration.Builder()
        .automaticallyCreateStream(true)
        .build();
    // @formatter:on
    statusEventsTunnel =
        new PubTransmitter(connectionPrimer, statusStreamConfiguration, pubOnlyConfiguration);
  }

  @EventListener(ApplicationReadyEvent.class)
  void onApplicationReadyEvent() {

    // Start subscription
    actionEventsTunnel.subscribe(this);
  }

  @Override
  public void subscriptionSucceeded(SubOnlyTunnel tunnel) {
    logger.info("Successfully subscribed to consume messages from NATS Jetstream.");
  }

  @Override
  @SuppressWarnings("squid:S2142")
  public void subscriptionFailed(SubOnlyTunnel tunnel, Exception exception) {
    logger.debug("Failed to subscribe for consuming messages from NATS Jetstream. Resubscribing...",
        exception);
    try {
      Thread.sleep(
          properties.getJetstream().getActionEvents().getConsumer().getResubWaitTime().toMillis());
    } catch (InterruptedException e) {
      logger.warn("Sleep failed: resubscribing without a waiting time...", e);
    } finally {
      tunnel.subscribe(this);
    }
  }

  @Override
  public void newMessageReceived(SubOnlyTunnel tunnel, String subject, String message) {
    try {
      processNATSMessage(message);
    } catch (Exception e) {
      logger.error("An error occurred during NATS message processing! Message dropped!", e);
    }
  }

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

    // Check the custom events are activated
    if (Boolean.FALSE.equals(isCustomEventEnabled(event))) {
      throw new BoomerangException(BoomerangError.WORKFLOW_TRIGGER_DISABLED);
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
        executionRequest.setProperties(
            processProperties(eventTrigger.getProperties(), eventTrigger.getWorkflowId()));

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

  @Override
  public void processNATSMessage(String message) throws InvalidPropertiesFormatException {
    CloudEvent cloudEvent =
        eventFormatProvider.resolveFormat(JsonFormat.CONTENT_TYPE).deserialize(message.getBytes());
    processCloudEventRequest(cloudEvent);
  }

  @Override
  public Future<Boolean> publishStatusCloudEvent(ActivityEntity activityEntity) {

    Supplier<Boolean> supplier = () -> {
      Boolean success = Boolean.FALSE;

      try {

        // Create status update CloudEvent from activity (additionally, add the responses for
        // executed tasks)
        EventStatusUpdate eventStatusUpdate =
            EventFactory.buildStatusUpdateFromActivity(activityEntity);
        eventStatusUpdate
            .setExecutedTasks(flowActivityService.getTaskExecutions(activityEntity.getId()));

        // Extract initiator ID and initiator context
        String initiatorId = "";

        if (activityEntity.getLabels() != null && !activityEntity.getLabels().isEmpty()) {
          String sharedLabelPrefix = properties.getShared().getLabel().getPrefix() + "/";

          initiatorId = activityEntity.getLabels().stream()
              .filter(kv -> kv.getKey().equals(sharedLabelPrefix + LABEL_KEY_INITIATOR_ID))
              .findFirst().map(KeyValuePair::getValue).orElse("");

          activityEntity.getLabels().stream()
              .filter(kv -> kv.getKey().equals(sharedLabelPrefix + LABEL_KEY_INITIATOR_CONTEXT))
              .findFirst().map(KeyValuePair::getValue)
              .ifPresent(eventStatusUpdate::setInitiatorContext);
        }

        // NATS message subject
        String nonWildcardSubject = getNonWildcardSubject(
            properties.getJetstream().getStatusEvents().getStream().getSubjects()[0]);
        String natSubject = MessageFormat.format("{0}.{1}.{2}.{3}{4}", nonWildcardSubject,
            eventStatusUpdate.getStatus().toString().toLowerCase(),
            eventStatusUpdate.getWorkflowId(), eventStatusUpdate.getWorkflowActivityId(),
            Strings.isNotEmpty(initiatorId) ? "." + initiatorId : "");

        // Publish cloud event
        String serializedCloudEvent = new String(eventFormatProvider
            .resolveFormat(JsonFormat.CONTENT_TYPE).serialize(eventStatusUpdate.toCloudEvent()));
        statusEventsTunnel.publish(natSubject, serializedCloudEvent);
        success = Boolean.TRUE;

        logger.debug("Workflow with ID {} has changed its status to {}",
            eventStatusUpdate.getWorkflowId(), activityEntity.getStatus());

      } catch (IllegalStateException | IOException | JetStreamApiException e) {
        logger.error("An exception occurred while publishing the message to NATS server!", e);
      } catch (StreamNotFoundException | SubjectMismatchException e) {
        logger.error("Stream is not configured properly!", e);
      } catch (Exception e) {
        logger.fatal("A fatal error has occurred while publishing the message to the NATS server!",
            e);
      }

      return success;
    };

    return CompletableFuture.supplyAsync(supplier);
  }

  @Override
  public Future<Boolean> publishStatusCloudEvent(TaskExecutionEntity taskExecutionEntity) {
    // TODO To be implemented!
    return CompletableFuture.supplyAsync(() -> Boolean.FALSE);
  }

  /**
   * Loop through a Workflow's parameters and if a JsonPath is set, read the event payload attempt
   * to find parameters.
   */
  private Map<String, String> processProperties(Map<String, String> eventProperties,
      String workflowId) {
    List<WorkflowProperty> inputProperties =
        workflowService.getWorkflow(workflowId).getProperties();
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

    processedProperties.forEach((k, v) -> {
      logger.info("processProperties() - {}={}", k, v);
    });

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

  private Boolean isCustomEventEnabled(Event event) {

    try {
      String workflowId = getWorkflowIdFromEvent(event);
      Triggers triggers = workflowService.getWorkflow(workflowId).getTriggers();

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

  private String getNonWildcardSubject(String subject) {
    int singleTokenCharIdx = subject.indexOf('*');
    int multipleTokensCharIdx = subject.indexOf('>');
    int charIdx = Integer.MAX_VALUE;

    if (singleTokenCharIdx >= 0) {
      charIdx = Math.min(charIdx, singleTokenCharIdx);
    }

    if (multipleTokensCharIdx >= 0) {
      charIdx = Math.min(charIdx, multipleTokensCharIdx);
    }

    if (charIdx >= 0 && charIdx < subject.length()) {
      int cutIdx = Math.max(0, charIdx - 1);
      return subject.substring(0, cutIdx);
    }

    return subject;
  }
}
