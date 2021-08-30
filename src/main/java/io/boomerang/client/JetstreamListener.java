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

  private static final String JETSTREAM_SUBJECT_SUFFIX = "cloudevent";

  @Autowired
  private JetstreamClient jetstreamClient;

  @Value("${eventing.jetstream.flow-subject-prefix}")
  private String jetstreamMessageSubjectPrefix;

  @Autowired
  private EventProcessor eventProcessor;

  @EventListener(ApplicationReadyEvent.class)
  void onApplicationReadyEvent() throws InterruptedException {
    String subject = jetstreamMessageSubjectPrefix + "." + JETSTREAM_SUBJECT_SUFFIX;

    logger.info("Initializng subscriptions to NATS Jetstream!");

    jetstreamClient.subscribe(subject, ConsumerType.PullBased,
        (messageSubject, message) -> eventProcessor.processNATSMessage(message));
  }
}
