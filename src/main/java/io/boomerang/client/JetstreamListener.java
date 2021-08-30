package io.boomerang.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import io.boomerang.jetstream.ConsumerType;
import io.boomerang.jetstream.JetstreamClient;
import io.boomerang.service.EventProcessor;

@Component
public class JetstreamListener {

  private static final Logger logger = LogManager.getLogger(JetstreamListener.class);

  @Autowired
  private JetstreamClient jetstreamClient;

  @Value("${eventing.jetstream.stream.subject}")
  private String jetstreamStreamSubject;

  @Autowired
  private EventProcessor eventProcessor;

  @EventListener(ApplicationReadyEvent.class)
  void onApplicationReadyEvent() throws InterruptedException {
    logger.info("Initializing subscriptions to NATS Jetstream!");

    jetstreamClient.subscribe(jetstreamStreamSubject, ConsumerType.PullBased,
        (subject, message) -> eventProcessor.processNATSMessage(message));
  }
}
