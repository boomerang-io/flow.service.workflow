package net.boomerangplatform.service;

import org.springframework.http.ResponseEntity;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;

public interface WebhookService {
  public FlowActivity getFlowActivity( String activityId);

  public FlowWebhookResponse submitWebhookEvent(RequestFlowExecution request);

  public ResponseEntity<FlowActivity> terminateActivity(String activityId);

}
