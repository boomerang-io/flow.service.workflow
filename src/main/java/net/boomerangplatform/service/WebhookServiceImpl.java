package net.boomerangplatform.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.controller.ActivityController;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.service.FlowWorkflowService;

@Service
public class WebhookServiceImpl implements WebhookService {

  @Autowired
  private FlowWorkflowService flowWorkflowService;


  @Autowired
  private ExecutionService executionService;
  
  @Autowired
  private ActivityController activityController;
  
  @Override
  public FlowWebhookResponse submitWebhookEvent(RequestFlowExecution request) {
    String tokenId = request.getToken();
    WorkflowEntity entity = flowWorkflowService.findByTokenString(tokenId);
    String workflowId = entity.getId(); 
    FlowExecutionRequest executionRequest = new FlowExecutionRequest();
    executionRequest.setProperties(request.getProperties());

    FlowActivity activity =  executionService.executeWorkflow(workflowId, Optional.of(FlowTriggerEnum.webhook), Optional.of(executionRequest));
    FlowWebhookResponse response = new FlowWebhookResponse();
    if (activity != null) {
      response.setActivityId(activity.getId());
    }
    return response;
  }

  @Override
  public FlowActivity getFlowActivity(String token, String activityId) {
    String tokenId = token;
    WorkflowEntity entity = flowWorkflowService.findByTokenString(tokenId);
    if (entity == null) {
      return null;
    }
    return activityController.getFlowActivity(activityId);
  }
}
