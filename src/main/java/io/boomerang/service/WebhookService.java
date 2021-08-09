package io.boomerang.service;

import org.springframework.http.ResponseEntity;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowWebhookResponse;
import io.boomerang.model.RequestFlowExecution;

public interface WebhookService {
  public FlowActivity getFlowActivity( String activityId);

  public FlowWebhookResponse submitWebhookEvent(RequestFlowExecution request);

  public ResponseEntity<FlowActivity> terminateActivity(String activityId);

}
