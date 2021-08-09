package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import io.boomerang.controller.ActivityController;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.FlowWebhookResponse;
import io.boomerang.model.RequestFlowExecution;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.FlowTriggerEnum;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.mongo.service.FlowWorkflowService;

@Service
public class WebhookServiceImpl implements WebhookService {

  @Autowired
  private FlowWorkflowService flowWorkflowService;


  @Autowired
  private ExecutionService executionService;

  @Autowired
  private ActivityController activityController;

  @Autowired
  private FlowWorkflowActivityService activityService;

  @Override
  public FlowWebhookResponse submitWebhookEvent(RequestFlowExecution request) {

    String workflowId = request.getWorkflowId();

    if (workflowId == null) {
      String tokenId = request.getToken();
      if (tokenId != null) {
        WorkflowEntity entity = flowWorkflowService.findByTokenString(tokenId);
        workflowId = entity.getId();
      }
    }

    FlowExecutionRequest executionRequest = new FlowExecutionRequest();
    executionRequest.setProperties(request.getProperties());
    executionRequest.setApplyQuotas(request.isApplyQuotas());
    
    Optional<List<TaskWorkspace>> workspaces = Optional.empty();

    if (request.getTaskWorkspaces() != null) {
      workspaces = Optional.of(request.getTaskWorkspaces());

    }
    FlowActivity activity = null;
    if (workflowId != null) {
      activity = executionService.executeWorkflow(workflowId,
          Optional.of(FlowTriggerEnum.webhook.toString()), Optional.of(executionRequest),
          workspaces);
    }
    FlowWebhookResponse response = new FlowWebhookResponse();
    if (activity != null) {
      response.setActivityId(activity.getId());
      WorkflowEntity workflow = flowWorkflowService.getWorkflow(workflowId);
      if (workflow.getFlowTeamId() != null) {
        activity.setTeamId(workflow.getFlowTeamId());
        activity.setScope(WorkflowScope.team);
      }
      else {
        activity.setScope(WorkflowScope.system);
      }
      activityService.saveWorkflowActivity(activity);
    }
    return response;
  }

  @Override
  public FlowActivity getFlowActivity(String activityId) {
    return activityController.getFlowActivity(activityId).getBody();
  }

  @Override
  public ResponseEntity<FlowActivity> terminateActivity(String activityId) {
    return activityController.cancelFlowActivity(activityId);
  }
}
