package net.boomerangplatform.controller;

import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.boomerangplatform.model.FlowActivity;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.entity.FlowTaskExecutionEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowActivityEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowEntity;
import net.boomerangplatform.mongo.entity.FlowWorkflowRevisionEntity;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;
import net.boomerangplatform.mongo.model.WorkflowStatus;
import net.boomerangplatform.mongo.service.FlowWorkflowVersionService;
import net.boomerangplatform.service.FlowExecutionService;
import net.boomerangplatform.service.crud.FlowActivityService;
import net.boomerangplatform.service.crud.WorkflowService;

@RestController
@RequestMapping("/flow/")
public class ExecutionController {

  private static final Logger LOGGER = LogManager.getLogger("ExecutionController");

  @Autowired
  private FlowActivityService activityService;

  @Autowired
  private FlowExecutionService flowExecutionService;

  @Autowired
  private FlowWorkflowVersionService flowRevisionService;

  @Autowired
  private WorkflowService workflowService;

  @PostMapping(value = "/execute/{workflowId}")
  public FlowActivity executeWorkflow(@PathVariable String workflowId,
      @RequestParam Optional<FlowTriggerEnum> trigger,
      @RequestBody Optional<FlowExecutionRequest> executionRequest) {

    final FlowWorkflowEntity newEntity = workflowService.getWorkflow(workflowId);
    
    if(!workflowService.canExecuteWorkflow(newEntity.getFlowTeamId())) {
      LOGGER.error("HTTP 429");
    } else {
      
      if (newEntity != null && newEntity.getStatus() == WorkflowStatus.active) {
        
        FlowExecutionRequest request = null;
  
        if (executionRequest.isPresent()) {
          request = executionRequest.get();
          logPayload(request);
        } else {
          request = new FlowExecutionRequest();
        }
  
        final FlowWorkflowRevisionEntity entity =
            this.flowRevisionService.getLatestWorkflowVersion(workflowId);
        if (entity != null) {
          final FlowWorkflowActivityEntity activity =
              activityService.createFlowActivity(entity.getId(), trigger, request);
          flowExecutionService.executeWorkflowVersion(entity.getId(), activity.getId());
          final List<FlowTaskExecutionEntity> steps =
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
    return null;

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
