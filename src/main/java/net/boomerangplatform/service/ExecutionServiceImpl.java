package net.boomerangplatform.service;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.error.BoomerangError;
import net.boomerangplatform.error.BoomerangException;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.entity.ActivityEntity;
import net.boomerangplatform.mongo.entity.RevisionEntity;
import net.boomerangplatform.mongo.entity.TaskExecutionEntity;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.model.WorkflowStatus;
import net.boomerangplatform.mongo.service.RevisionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.crud.WorkflowService;

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

  public FlowActivity executeWorkflow(String workflowId,
      Optional<String> trigger,
      Optional<FlowExecutionRequest> executionRequest) {
    
    final WorkflowEntity workflow = workflowService.getWorkflow(workflowId);

    if (!workflowService.canExecuteWorkflow(workflowId,trigger)) {
      throw new BoomerangException(BoomerangError.WORKLOAD_MANUAL_DISABLED);
    }
    else if (!workflowService.canExecuteWorkflowForQuotas(workflow.getFlowTeamId())) {
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
          final ActivityEntity activity =
              activityService.createFlowActivity(entity.getId(), trigger, request);
          flowExecutionService.executeWorkflowVersion(entity.getId(), activity.getId());
          final List<TaskExecutionEntity> steps =
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
