package io.boomerang.service;

import java.util.InvalidPropertiesFormatException;
import io.cloudevents.CloudEvent;

public interface CloudEventsService {
  void processCloudEventRequest(CloudEvent cloudEvent) throws InvalidPropertiesFormatException;
}
