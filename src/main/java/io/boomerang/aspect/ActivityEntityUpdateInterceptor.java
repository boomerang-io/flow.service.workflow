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
import io.boomerang.mongo.repository.FlowWorkflowActivityRepository;
import io.boomerang.service.EventingService;

@Aspect
@Component
public class ActivityEntityUpdateInterceptor {

  private static final Logger logger = LogManager.getLogger(ActivityEntityUpdateInterceptor.class);

  @Autowired
  FlowWorkflowActivityRepository activityRepository;

  @Autowired
  private EventingService eventingService;

  @Before("execution(* io.boomerang.mongo.repository.FlowWorkflowActivityRepository.save(..))"
      + " && args(entityToBeSaved)")
  public void beforeSaveInvoked(JoinPoint thisJoinPoint, Object entityToBeSaved) {

    logger.info("Intercepted save action on entity {} from {}", entityToBeSaved,
        thisJoinPoint.getSignature().getDeclaringTypeName());

    if (entityToBeSaved instanceof ActivityEntity) {
      activityEntityToBeUpdated((ActivityEntity) entityToBeSaved);
    }
  }

  private void activityEntityToBeUpdated(ActivityEntity newActivityEntity) {

    // Check if activity and workflow IDs are not empty
    if (StringUtils.isNotBlank(newActivityEntity.getWorkflowId())
        && StringUtils.isNotBlank(newActivityEntity.getId())) {

      // Retrieve old entity and compare the statuses
      activityRepository.findById(newActivityEntity.getId()).ifPresent(oldActivityEntity -> {
        if (oldActivityEntity.getStatus() != newActivityEntity.getStatus()) {

          // Status has changed, publish status update CloudEvent
          eventingService.publishStatusCloudEvent(newActivityEntity);
        }
      });
    }
  }
}
