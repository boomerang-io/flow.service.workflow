package io.boomerang.service;

import io.cloudevents.CloudEvent;

public interface EventProcessor {

  void processCloudEventRequest(CloudEvent cloudEvent) throws Exception;

  void processNATSMessage(String payload) throws Exception;
}
