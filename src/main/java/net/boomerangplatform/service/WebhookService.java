package net.boomerangplatform.service;

import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;

public interface WebhookService {
  public FlowActivity getFlowActivity( String activityId);

  public FlowWebhookResponse submitWebhookEvent(RequestFlowExecution request);

}
