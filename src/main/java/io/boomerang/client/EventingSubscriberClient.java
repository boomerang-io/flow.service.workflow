package io.boomerang.client;

import java.time.Duration;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import io.boomerang.eventing.nats.ConnectionPrimer;
import io.boomerang.eventing.nats.jetstream.PubSubConfiguration;
import io.boomerang.eventing.nats.jetstream.PubSubTransceiver;
import io.boomerang.eventing.nats.jetstream.PubSubTunnel;
import io.boomerang.eventing.nats.jetstream.SubHandler;
import io.boomerang.service.EventProcessor;
import io.nats.client.Options;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

@Component
@ConditionalOnProperty(value = "eventing.enabled", havingValue = "true", matchIfMissing = false)
public class EventingSubscriberClient {

  private static final Logger logger = LogManager.getLogger(EventingSubscriberClient.class);

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

  @Autowired
  private EventProcessor eventProcessor;

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

    ConnectionPrimer connectionPrimer = new ConnectionPrimer(optionsBuilder);
    PubSubTunnel pubSubTunnel = new PubSubTransceiver(connectionPrimer, streamConfiguration,
        consumerConfiguration, pubSubConfiguration);

    startSubscription(pubSubTunnel);
  }

  private void startSubscription(PubSubTunnel tunnel) {

    tunnel.subscribe(new SubHandler() {

      @Override
      public void subscriptionSucceeded(PubSubTunnel tunnel) {
        logger.info("Successfully subscribed to consume messages from NATS Jetstream.");
      }

      @Override
      public void subscriptionFailed(PubSubTunnel tunnel, Exception exception) {
        logger.debug(
            "Failed to subscribe for consuming messages from NATS Jetstream. Resubscribing...");
        try {
          Thread.sleep(jetstreamConsumerResubscribeWaitTime.toMillis());
        } catch (Exception e) {
          logger.warn("Sleep failed: resubscribing without a waiting time...");
        } finally {
          startSubscription(tunnel);
        }
      }

      @Override
      public void newMessageReceived(PubSubTunnel tunnel, String subject, String message) {
        eventProcessor.processNATSMessage(message);
      }
    });
  }
}
