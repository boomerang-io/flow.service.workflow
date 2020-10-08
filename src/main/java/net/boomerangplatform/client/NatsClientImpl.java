package net.boomerangplatform.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import io.nats.streaming.Options;
import io.nats.streaming.StreamingConnection;
import io.nats.streaming.StreamingConnectionFactory;
import io.nats.streaming.Subscription;
import io.nats.streaming.SubscriptionOptions;

import net.boomerangplatform.service.EventProcessor;

@Component
public class NatsClientImpl implements NatsClient {

	@Value("${eventing.nats.url}")
	private String natsUrl;

	@Value("${eventing.nats.cluster}")
	private String natsCluster;

    @Value("${eventing.nats.channel}")
    private String natsChannel;

    protected static final String QUEUE = "flow-workflow";

	private StreamingConnection streamingConnection;

	private final Logger logger = LogManager.getLogger();

    @Autowired
    private EventProcessor eventProcessor;

//    TODO: better return management

//	TODO IF eventing enabled, start this on application startup OR is this what @EventListener is doing?
	@EventListener
	public void subscribe(ContextRefreshedEvent event) throws TimeoutException {
	    
	    int random = (int) (Math.random() * 10000 + 1); // NOSONAR

	      logger.info("Initializng subscriptions to NATS with URL: " + natsUrl + ", Cluster: " + natsCluster + ", Client ID: " + "flow-workflow-" + random);

        Options cfOptions = new Options.Builder().natsUrl(natsUrl).clusterId(natsCluster).clientId("flow-workflow-" + random).build();
        StreamingConnectionFactory cf = new StreamingConnectionFactory(cfOptions);
        
        try {
          this.streamingConnection = cf.createConnection();

          Subscription subscription =
              streamingConnection.subscribe(natsChannel, QUEUE, new MessageHandler() { // NOSONAR
                @Override
                public void onMessage(Message m) {
                  eventProcessor.processNATSMessage(new String(m.getData()));
                }
              }, new SubscriptionOptions.Builder().durableName("durable").build());
        } catch (IOException ex) {
          logger.error(ex);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
//        TODO do we close connection and subscription?
	}
	
}
