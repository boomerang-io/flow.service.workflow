package net.boomerangplatform.client;

import io.nats.streaming.Message;

public interface NatsClient {
	public void processMessage(Message msg);

}
