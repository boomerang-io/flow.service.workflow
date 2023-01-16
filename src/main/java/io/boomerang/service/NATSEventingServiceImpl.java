package io.boomerang.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import io.boomerang.config.EventingProperties;
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
import io.boomerang.model.eventing.EventFactory;
import io.boomerang.model.eventing.EventTaskStatusUpdate;
import io.boomerang.model.eventing.EventWorkflowStatusUpdate;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.refactor.TaskService;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.nats.client.JetStreamApiException;
import io.nats.client.Options;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StreamConfiguration;

@Service
@ConditionalOnProperty(value = "nats.eventing.enabled", havingValue = "true",
    matchIfMissing = false)
public class NATSEventingServiceImpl implements NATSEventingService, SubHandler {

  private static final Logger logger = LogManager.getLogger(NATSEventingServiceImpl.class);

  protected static final String LABEL_KEY_WORKFLOW_ID = "workflowId";

  protected static final String LABEL_KEY_ACTIVITY_ID = "activityId";

  protected static final String LABEL_KEY_TASK_ID = "taskId";

  protected static final String LABEL_KEY_STATUS = "status";

  @Autowired
  EventingProperties properties;

  @Lazy
  @Autowired
  private FlowActivityService flowActivityService;

  @Lazy
  @Autowired
  private TaskService taskService;

  @Lazy
  @Autowired
  private CloudEventsService cloudEventsService;

  private PubOnlyTunnel outputEventsTunnel;

  private SubOnlyTunnel inputEventsTunnel;

  private EventFormatProvider eventFormatProvider = EventFormatProvider.getInstance();

  @PostConstruct
  private void init() {

    ConnectionPrimer connectionPrimer;

    // Build connection primer
    // @formatter:off
    Options.Builder optionsBuilder = new Options.Builder()
        .servers(properties.getNats().getServer().getUrls())
        .reconnectWait(properties.getNats().getServer().getReconnectWaitTime())
        .maxReconnects(properties.getNats().getServer().getReconnectMaxAttempts());
    // @formatter:on
    connectionPrimer = new ConnectionPrimer(optionsBuilder);

    // Create the tunnel for input CloudEvents (trigger, wait for event, cancel, etc.)
    // @formatter:off
    StreamConfiguration inputStreamConfiguration = new StreamConfiguration.Builder()
        .name(properties.getJetstream().getInputEvents().getStream().getName())
        .storageType(properties.getJetstream().getInputEvents().getStream().getStorageType())
        .subjects(properties.getJetstream().getInputEvents().getStream().getSubjects())
        .build();
    ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration.Builder()
        .durable(properties.getJetstream().getInputEvents().getConsumer().getName())
        .build();
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration.Builder()
        .automaticallyCreateStream(true)
        .automaticallyCreateConsumer(true)
        .build();
    // @formatter:on
    inputEventsTunnel = new PubSubTransceiver(connectionPrimer, inputStreamConfiguration,
        consumerConfiguration, pubSubConfiguration);

    // Create the tunnel for output CloudEvents (workflow status update, task status update)
    // @formatter:off
    StreamConfiguration outputStreamConfiguration = new StreamConfiguration.Builder()
        .name(properties.getJetstream().getOutputEvents().getStream().getName())
        .storageType(properties.getJetstream().getOutputEvents().getStream().getStorageType())
        .subjects(properties.getJetstream().getOutputEvents().getStream().getSubjects())
        .build();
    PubOnlyConfiguration pubOnlyConfiguration = new PubOnlyConfiguration.Builder()
        .automaticallyCreateStream(true)
        .build();
    // @formatter:on
    outputEventsTunnel =
        new PubTransmitter(connectionPrimer, outputStreamConfiguration, pubOnlyConfiguration);
  }

  @EventListener(ApplicationReadyEvent.class)
  void onApplicationReadyEvent() {

    // Start subscription
    inputEventsTunnel.subscribe(this);
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
          properties.getJetstream().getInputEvents().getConsumer().getResubWaitTime().toMillis());
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
  public void processNATSMessage(String message) throws InvalidPropertiesFormatException {
    CloudEvent cloudEvent =
        eventFormatProvider.resolveFormat(JsonFormat.CONTENT_TYPE).deserialize(message.getBytes());
    cloudEventsService.processCloudEventRequest(cloudEvent);
  }

  @Override
  public Future<Boolean> publishStatusCloudEvent(ActivityEntity activityEntity) {

    Supplier<Boolean> supplier = () -> {
      Boolean isSuccess = Boolean.FALSE;

      try {
        // Create status update CloudEvent from activity (additionally, add the responses for
        // executed tasks)
        EventWorkflowStatusUpdate eventStatusUpdate =
            EventFactory.buildStatusUpdateEvent(activityEntity);
        eventStatusUpdate
            .setExecutedTasks(flowActivityService.getTaskExecutions(activityEntity.getId()));
        String initiatorId = "";

        // Extract initiator ID and initiator context
        if (activityEntity.getLabels() != null && !activityEntity.getLabels().isEmpty()) {
          String sharedLabelPrefix = properties.getShared().getLabel().getPrefix() + "/";

          initiatorId = activityEntity.getLabels().stream()
              .filter(kv -> kv.getKey()
                  .equals(sharedLabelPrefix + CloudEventsServiceImpl.LABEL_KEY_INITIATOR_ID))
              .findFirst().map(KeyValuePair::getValue).orElse("");

          activityEntity.getLabels().stream()
              .filter(kv -> kv.getKey()
                  .equals(sharedLabelPrefix + CloudEventsServiceImpl.LABEL_KEY_INITIATOR_CONTEXT))
              .findFirst().map(KeyValuePair::getValue)
              .ifPresent(eventStatusUpdate::setInitiatorContext);
        }

        // Generate NATS message subject and publish cloud event
        String natsSubject = generateNATSSubject(activityEntity, initiatorId);
        String serializedCloudEvent = new String(eventFormatProvider
            .resolveFormat(JsonFormat.CONTENT_TYPE).serialize(eventStatusUpdate.toCloudEvent()));

        outputEventsTunnel.publish(natsSubject, serializedCloudEvent);
        isSuccess = Boolean.TRUE;

        logger.debug("Workflow with ID {} has changed its status to {}",
            eventStatusUpdate.getWorkflowId(), eventStatusUpdate.getStatus());

      } catch (IllegalStateException | IOException | JetStreamApiException e) {
        logger.error("An exception occurred while publishing the message to NATS server!", e);
      } catch (StreamNotFoundException | SubjectMismatchException e) {
        logger.error("Stream is not configured properly!", e);
      } catch (Exception e) {
        logger.fatal("A fatal error has occurred while publishing the message to the NATS server!",
            e);
      }
      return isSuccess;
    };

    return CompletableFuture.supplyAsync(supplier);
  }

  @Override
  public Future<Boolean> publishStatusCloudEvent(TaskExecutionEntity taskExecutionEntity,
      ActivityEntity parentActivityEntity) {

    Supplier<Boolean> supplier = () -> {
      Boolean isSuccess = Boolean.FALSE;

      try {
        // If task execution entity is missing workflow ID, add it
        if (StringUtils.isBlank(taskExecutionEntity.getWorkflowId())) {
          taskExecutionEntity.setWorkflowId(parentActivityEntity.getWorkflowId());
        }

        // Retrieve WFE task topic if task is of type WFE
        String taskWfeTopic = taskService.retrieveWaitForEventTaskTopic(taskExecutionEntity);
        Map<String, String> additionalData = new HashMap<>();

        if (StringUtils.isNotBlank(taskWfeTopic)) {
          additionalData.put("wfetopic", taskWfeTopic);
        }

        // Create status update CloudEvent from task execution
        EventTaskStatusUpdate eventStatusUpdate =
            EventFactory.buildStatusUpdateEvent(taskExecutionEntity, additionalData);
        String initiatorId = "";

        // Extract initiator ID and initiator context
        if (parentActivityEntity.getLabels() != null
            && !parentActivityEntity.getLabels().isEmpty()) {
          String sharedLabelPrefix = properties.getShared().getLabel().getPrefix() + "/";

          initiatorId = parentActivityEntity.getLabels().stream()
              .filter(kv -> kv.getKey()
                  .equals(sharedLabelPrefix + CloudEventsServiceImpl.LABEL_KEY_INITIATOR_ID))
              .findFirst().map(KeyValuePair::getValue).orElse("");

          parentActivityEntity.getLabels().stream()
              .filter(kv -> kv.getKey()
                  .equals(sharedLabelPrefix + CloudEventsServiceImpl.LABEL_KEY_INITIATOR_CONTEXT))
              .findFirst().map(KeyValuePair::getValue)
              .ifPresent(eventStatusUpdate::setInitiatorContext);
        }

        // Generate NATS message subject and publish cloud event
        String natsSubject = generateNATSSubject(taskExecutionEntity, initiatorId);
        String serializedCloudEvent = new String(eventFormatProvider
            .resolveFormat(JsonFormat.CONTENT_TYPE).serialize(eventStatusUpdate.toCloudEvent()));

        outputEventsTunnel.publish(natsSubject, serializedCloudEvent);
        isSuccess = Boolean.TRUE;

        logger.debug("Task with ID {} has changed its status to {}", eventStatusUpdate.getTaskId(),
            eventStatusUpdate.getStatus());

      } catch (IllegalStateException | IOException | JetStreamApiException e) {
        logger.error("An exception occurred while publishing the message to NATS server!", e);
      } catch (StreamNotFoundException | SubjectMismatchException e) {
        logger.error("Stream is not configured properly!", e);
      } catch (Exception e) {
        logger.fatal("A fatal error has occurred while publishing the message to the NATS server!",
            e);
      }
      return isSuccess;
    };

    return CompletableFuture.supplyAsync(supplier);
  }

  private String generateNATSSubject(ActivityEntity activityEntity, String initiatorId) {

    // Create the token map for NATS subject pattern components
    // @formatter:off
    Map<String, String> natsSubjectTokens = new HashMap<>(Map.of(
        CloudEventsServiceImpl.LABEL_KEY_INITIATOR_ID, initiatorId,
        LABEL_KEY_WORKFLOW_ID, activityEntity.getWorkflowId(),
        LABEL_KEY_ACTIVITY_ID, activityEntity.getId(),
        LABEL_KEY_STATUS, activityEntity.getStatus().toString().toLowerCase()));
    // @formatter:on

    return normalizePatternTokens(
        properties.getJetstream().getOutputEvents().getSubjectPattern().getWorkflowStatus(),
        natsSubjectTokens, ".");
  }

  private String generateNATSSubject(TaskExecutionEntity taskExecutionEntity, String initiatorId) {

    // Create the token map for NATS subject pattern components
    // @formatter:off
    Map<String, String> natsSubjectTokens = new HashMap<>(Map.of(
        LABEL_KEY_TASK_ID, taskExecutionEntity.getId(),
        CloudEventsServiceImpl.LABEL_KEY_INITIATOR_ID, initiatorId,
        LABEL_KEY_WORKFLOW_ID, taskExecutionEntity.getWorkflowId(),
        LABEL_KEY_ACTIVITY_ID, taskExecutionEntity.getActivityId(),
        LABEL_KEY_STATUS, taskExecutionEntity.getFlowTaskStatus().toString().toLowerCase()));
    // @formatter:on

    return normalizePatternTokens(
        properties.getJetstream().getOutputEvents().getSubjectPattern().getTaskStatus(),
        natsSubjectTokens, ".");
  }

  private String normalizePatternTokens(List<String> patternTokens, Map<String, String> tokens,
      String joinDelimiter) {

    // @formatter:off
    UnaryOperator<String> patternMatcher = component -> Stream.of(component)
        .filter(str -> str.matches("^%.*%$"))
        .map(str -> str.replaceAll("(^%)|(%$)", ""))
        .filter(tokens::containsKey)
        .map(tokens::get)
        .findAny()
        .orElse(component);
    // @formatter:on

    return patternTokens.stream().map(patternMatcher).filter(Strings::isNotBlank)
        .collect(Collectors.joining(joinDelimiter));
  }
}
