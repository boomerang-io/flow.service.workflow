package io.boomerang.service;

import io.boomerang.mongo.entity.ActivityEntity;
import io.cloudevents.CloudEvent;

public interface EventingService {

  void processCloudEventRequest(CloudEvent cloudEvent) throws Exception;

  void processNATSMessage(String payload) throws Exception;

  /**
   * This method will publish a Cloud Event encoded as a string to the NATS server. Please make sure
   * the status of the {@link ActivityEntity} is updated when invoking this method.
   * 
   * @param activityEntity Activity entity.
   * 
   * @Note Do not invoke this method with if the status of the {@link ActivityEntity} has not been
   *       changed, as this would result in publishing a Cloud Event with the same status multiple
   *       times.
   */
  void publishActivityStatusEvent(ActivityEntity activityEntity);
}
