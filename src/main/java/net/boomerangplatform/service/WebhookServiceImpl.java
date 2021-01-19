package net.boomerangplatform.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import net.boomerangplatform.controller.ActivityController;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.model.FlowWebhookResponse;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.model.controller.TaskWorkspace;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.model.WorkflowScope;
import net.boomerangplatform.mongo.service.FlowWorkflowActivityService;
import net.boomerangplatform.mongo.service.FlowWorkflowService;

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
}
