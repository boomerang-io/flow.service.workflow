package io.boomerang.aspect;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.boomerang.mongo.entity.ActivityEntity;
import io.boomerang.mongo.entity.TaskExecutionEntity;
import io.boomerang.mongo.repository.FlowWorkflowActivityTaskRepository;
import io.boomerang.mongo.service.FlowWorkflowActivityService;
import io.boomerang.service.EventingService;

@Aspect
@Component
public class TaskExecutionUpdateInterceptor {

  private static final Logger logger = LogManager.getLogger(TaskExecutionUpdateInterceptor.class);

  @Autowired
  FlowWorkflowActivityTaskRepository taskRepository;

  @Autowired
  private FlowWorkflowActivityService activityService;

  @Autowired
  private EventingService eventingService;

  @Before("execution(* io.boomerang.mongo.repository.FlowWorkflowActivityTaskRepository.save(..))"
      + " && args(entityToBeSaved)")
  public void beforeSaveInvoked(JoinPoint thisJoinPoint, Object entityToBeSaved) {

    logger.info("Intercepted save action on entity {} from {}", entityToBeSaved,
        thisJoinPoint.getSignature().getDeclaringTypeName());

    if (entityToBeSaved instanceof TaskExecutionEntity) {
      taskExecutionToBeUpdated((TaskExecutionEntity) entityToBeSaved);
    }
  }

  private void taskExecutionToBeUpdated(TaskExecutionEntity newTaskExecution) {

    logger.info("-------------------------------- {}, {}, {}, {}", newTaskExecution.getId(),
        newTaskExecution.getActivityId(), newTaskExecution.getRunWorkflowId(),
        newTaskExecution.getRunWorkflowId());

    // Check if task and activity IDs are not empty
    if (StringUtils.isNotBlank(newTaskExecution.getActivityId())
        && StringUtils.isNotBlank(newTaskExecution.getId())) {

      logger.info("-------------------------------- search");

      // Retrieve old entity and compare the statuses
      taskRepository.findById(newTaskExecution.getId()).ifPresentOrElse(oldTaskExecution -> {

        logger.info("-------------------------------- found! {}, {} -> {}",
            newTaskExecution.getId(), oldTaskExecution.getFlowTaskStatus(),
            newTaskExecution.getFlowTaskStatus());

        if (oldTaskExecution.getFlowTaskStatus() != newTaskExecution.getFlowTaskStatus()) {

          // Status has changed, publish status update CloudEvent
          ActivityEntity activityEntity =
              activityService.findWorkflowActivtyById(newTaskExecution.getActivityId());
          eventingService.publishStatusCloudEvent(newTaskExecution, activityEntity);
        }
      }, () -> {
        logger.info("-------------------------------- NOT found! {}", newTaskExecution.getId());
      });
    }
  }
}
