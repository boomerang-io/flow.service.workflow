package io.boomerang.service;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import io.boomerang.eventing.nats.ConnectionPrimer;
import io.boomerang.eventing.nats.jetstream.PubOnlyConfiguration;
import io.boomerang.eventing.nats.jetstream.PubOnlyTunnel;
import io.boomerang.eventing.nats.jetstream.PubSubConfiguration;
import io.boomerang.eventing.nats.jetstream.PubSubTransceiver;
import io.boomerang.eventing.nats.jetstream.PubSubTunnel;
import io.boomerang.eventing.nats.jetstream.PubTransmitter;
import io.boomerang.eventing.nats.jetstream.SubHandler;
import io.boomerang.eventing.nats.jetstream.SubOnlyTunnel;
import io.boomerang.eventing.nats.jetstream.exception.StreamNotFoundException;
import io.boomerang.eventing.nats.jetstream.exception.SubjectMismatchException;
import io.boomerang.mongo.entity.ActivityEntity;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.provider.EventFormatProvider;
import io.cloudevents.jackson.JsonFormat;
import io.nats.client.JetStreamApiException;
import io.nats.client.Options;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

@Service
@ConditionalOnProperty(value = "eventing.enabled", havingValue = "true", matchIfMissing = false)
public class EventingServiceImpl implements EventingService, SubHandler {

  private static final Logger logger = LogManager.getLogger(EventingServiceImpl.class);

  @Value("#{'${eventing.nats.server.urls}'.split(',')}")
  private List<String> serverUrls;

  @Value("${eventing.nats.server.reconnect-wait-time:PT10S}")
  private Duration serverReconnectWaitTime;

  @Value("${eventing.nats.server.reconnect-max-attempts:-1}")
  private Integer serverReconnectMaxAttempts;

  @Value("${eventing.jetstream.stream.name}")
  private String jetstreamStreamName;

  @Value("${eventing.jetstream.stream.storage-type}")
  private StorageType jetstreamStreamStorageType;

  @Value("${eventing.jetstream.stream.subject}")
  private String jetstreamStreamSubject;

  @Value("${eventing.jetstream.consumer.name}")
  private String jetstreamConsumerDurableName;

  @Value("${eventing.jetstream.consumer.resub-wait-time:PT10S}")
  private Duration jetstreamConsumerResubscribeWaitTime;

  @Lazy
  @Autowired
  private EventProcessor eventProcessor;

  private ConnectionPrimer connectionPrimer;

  private PubOnlyTunnel pubOnlyTunnel;

  @EventListener(ApplicationReadyEvent.class)
  void onApplicationReadyEvent() throws InterruptedException {

    // @formatter:off
    Options.Builder optionsBuilder = new Options.Builder()
        .servers(serverUrls.toArray(new String[0]))
        .reconnectWait(serverReconnectWaitTime)
        .maxReconnects(serverReconnectMaxAttempts);
    StreamConfiguration streamConfiguration = new StreamConfiguration.Builder()
        .name(jetstreamStreamName)
        .storageType(jetstreamStreamStorageType)
        .subjects(jetstreamStreamSubject)
        .build();
    ConsumerConfiguration consumerConfiguration = new ConsumerConfiguration.Builder()
        .durable(jetstreamConsumerDurableName)
        .build();
    PubSubConfiguration pubSubConfiguration = new PubSubConfiguration.Builder()
        .automaticallyCreateStream(true)
        .automaticallyCreateConsumer(true)
        .build();
    // @formatter:on

    connectionPrimer = new ConnectionPrimer(optionsBuilder);
    PubSubTunnel pubSubTunnel = new PubSubTransceiver(connectionPrimer, streamConfiguration,
        consumerConfiguration, pubSubConfiguration);

    // Start subscription
    pubSubTunnel.subscribe(this);
  }

  @Override
  public void subscriptionSucceeded(SubOnlyTunnel tunnel) {
    logger.info("Successfully subscribed to consume messages from NATS Jetstream.");
  }

  @Override
  public void subscriptionFailed(SubOnlyTunnel tunnel, Exception exception) {
    logger.debug("Failed to subscribe for consuming messages from NATS Jetstream. Resubscribing...",
        exception);
    try {
      Thread.sleep(jetstreamConsumerResubscribeWaitTime.toMillis());
    } catch (Exception e) {
      logger.warn("Sleep failed: resubscribing without a waiting time...", e);
    } finally {
      tunnel.subscribe(this);
    }
  }

  @Override
  public void newMessageReceived(SubOnlyTunnel tunnel, String subject, String message) {
    try {
      eventProcessor.processNATSMessage(message);
    } catch (Exception e) {
    }
  }

  /**
   * This method will publish a Cloud Event encoded as a string to the NATS server. Please make sure
   * the status of the {@link ActivityEntity} is updated when invoking this method.
   * 
   * @param activityEntity Activity entity.
   * 
   * @note Do not invoke this method with if the status of the {@link ActivityEntity} has not been
   *       changed, as this would result in publishing a Cloud Event with the same status multiple
   *       times.
   */
  @Override
  public void publishWorkflowActivityStatusUpdateCE(ActivityEntity activityEntity) {

    // Create cloud event
    String type = "io.boomerang.eventing.status-update";
    String id = UUID.randomUUID().toString();
    String subject = MessageFormat.format("/{0}/status/{1}", activityEntity.getWorkflowId(),
        activityEntity.getStatus());
    URI source = URI.create("io.boomerang.service.EventingServiceImpl");

    JsonObject jsonData = new JsonObject();
    jsonData.addProperty("status", activityEntity.getStatus().toString());

    // @formatter:off
    CloudEvent cloudEvent = CloudEventBuilder.v03()
        .withType(type)
        .withId(id)
        .withSubject(subject)
        .withSource(source)
        .withData("application/json", jsonData.toString().getBytes())
        .withTime(OffsetDateTime.now())
        .build();
    // @formatter:on

    // Publish cloud event
    String natsMessageSubject = MessageFormat.format("{0}.{1}", getStatusUpdateSubjectPrefix(),
        StringUtils.lowerCase(activityEntity.getStatus().toString()));

    try {
      String serializedCloudEvent = new String(EventFormatProvider.getInstance()
          .resolveFormat(JsonFormat.CONTENT_TYPE).serialize(cloudEvent));

      getPubOnlyTunnel().publish(natsMessageSubject, serializedCloudEvent);
    } catch (StreamNotFoundException | SubjectMismatchException e) {
      logger.error("Stream is not configured properly!", e);
    } catch (IllegalStateException | IOException | JetStreamApiException e) {
      logger.error("An exception occurred while publishing the message to NATS server!", e);
    }

    logger.debug("Workflow with ID " + activityEntity.getWorkflowId()
        + " has changed its status to " + activityEntity.getStatus());
  }

  private String getStatusUpdateSubjectPrefix() {
    final String statusUpdateSubjectSuffix = "status-update";

    if (jetstreamStreamSubject.endsWith(">")) {
      return StringUtils.chop(jetstreamStreamSubject) + statusUpdateSubjectSuffix;
    } else {
      return jetstreamStreamSubject + "." + statusUpdateSubjectSuffix;
    }
  }

  private PubOnlyTunnel getPubOnlyTunnel() {

    if (pubOnlyTunnel == null) {

      // @formatter:off
      StreamConfiguration streamConfiguration = new StreamConfiguration.Builder()
          .name(jetstreamStreamName + "-status")
          .storageType(jetstreamStreamStorageType)
          .subjects(getStatusUpdateSubjectPrefix() + ".>")
          .build();
      PubOnlyConfiguration pubOnlyConfiguration = new PubOnlyConfiguration.Builder()
          .automaticallyCreateStream(true)
          .build();
      // @formatter:on
      pubOnlyTunnel =
          new PubTransmitter(connectionPrimer, streamConfiguration, pubOnlyConfiguration);
    }

    return pubOnlyTunnel;
  }
}
