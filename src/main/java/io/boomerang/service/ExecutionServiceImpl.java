package io.boomerang.service;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.boomerang.error.BoomerangError;
import io.boomerang.error.BoomerangException;
import io.boomerang.model.FlowActivity;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.model.TaskExecutionResponse;
import io.boomerang.model.controller.TaskWorkspace;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.RevisionEntity;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.model.WorkflowScope;
import io.boomerang.mongo.model.WorkflowStatus;
import io.boomerang.mongo.service.RevisionService;
import io.boomerang.service.crud.FlowActivityService;
import io.boomerang.service.crud.WorkflowService;

@Service
public class ExecutionServiceImpl implements ExecutionService {
  private static final Logger LOGGER = LogManager.getLogger("ExecutionController");

  @Autowired
  private FlowActivityService activityService;

  @Autowired
  private FlowExecutionService flowExecutionService;

  @Autowired
  private RevisionService flowRevisionService;

  @Autowired
  private WorkflowService workflowService;

  @Override
  public FlowActivity executeWorkflow(String workflowId, Optional<String> trigger,
      Optional<FlowExecutionRequest> executionRequest,
      Optional<List<TaskWorkspace>> taskWorkspaces) {

    final WorkflowEntity workflow = workflowService.getWorkflow(workflowId);

    if (!workflowService.canExecuteWorkflow(workflowId, trigger)) {
      throw new BoomerangException(BoomerangError.WORKLOAD_MANUAL_DISABLED);
    } else if (WorkflowScope.team.equals(workflow.getScope())
        && !workflowService.canExecuteWorkflowForQuotas(workflow.getFlowTeamId())) {
      throw new BoomerangException(BoomerangError.TOO_MANY_REQUESTS);
    } else if (WorkflowScope.user.equals(workflow.getScope())
        && !workflowService.canExecuteWorkflowForQuotasForUser(workflowId)) {
      throw new BoomerangException(BoomerangError.TOO_MANY_REQUESTS);
    } else {
      if (workflow.getStatus() == WorkflowStatus.active) {
        FlowExecutionRequest request = null;
        if (executionRequest.isPresent()) {
          request = executionRequest.get();
          logPayload(request);
        } else {
          request = new FlowExecutionRequest();
        }

        final RevisionEntity entity = this.flowRevisionService.getLatestWorkflowVersion(workflowId);
        if (entity != null) {
          final ActivityEntity activity = activityService.createFlowActivity(entity.getId(),
              trigger, request, taskWorkspaces, request.getLabels());
          flowExecutionService.executeWorkflowVersion(entity.getId(), activity.getId());

          final List<TaskExecutionResponse> steps =
              activityService.getTaskExecutions(activity.getId());
          final FlowActivity response = new FlowActivity(activity);
          response.setSteps(steps);
          return response;
        } else {
          LOGGER.error("No revision to execute");
        }
        return null;
      } else {
        LOGGER.error("The workflow status is not active");
        return null;
      }
    }
  }

  private void logPayload(FlowExecutionRequest request) {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      String payload = objectMapper.writeValueAsString(request);
      LOGGER.info("Received Request Payload: ");
      LOGGER.info(payload);
    } catch (JsonProcessingException e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }
}
