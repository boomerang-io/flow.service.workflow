package io.boomerang.service.crud;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.mongo.model.TriggerScheduler;
import io.boomerang.mongo.model.Triggers;
import io.boomerang.mongo.model.WorkflowScheduleStatus;
import io.boomerang.mongo.service.FlowWorkflowScheduleService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.quartz.ScheduledTasks;

public class WorkflowScheduleServiceImpl {

  private final Logger logger = LogManager.getLogger(getClass());

  @Autowired
  private ScheduledTasks taskScheduler;

  @Autowired
  private FlowWorkflowScheduleService workflowScheduleRepository;

  @Autowired
  private FlowWorkflowService workflowRepository;

  private void scheduleWorkflow(final WorkflowEntity entity, boolean previous, boolean current) {
    if (!previous && current) {
      this.taskScheduler.scheduleWorkflow(entity);
    } else if (previous && !current) {
      try {
        this.taskScheduler.cancelJob(entity.getId());
      } catch (SchedulerException e) {
        logger.info("Unable to schedule job. ");
        logger.error(e);
      }
    } else if (current) {
      try {
        this.taskScheduler.cancelJob(entity.getId());
        this.taskScheduler.scheduleWorkflow(entity);

      } catch (SchedulerException e) {
        logger.info("Unable to reschedule job. ");
        logger.error(e);
      }
    }
  }
  
  private void updateSchedule(final WorkflowScheduleEntity entity) {
//    TODO: do we have to map a model to the entity for external consumption
    if (entity != null) {
      WorkflowEntity wfEntity = workflowRepository.getWorkflow(entity.getWorkflowId());
      if (wfEntity != null && wfEntity.getTriggers().getScheduler().getEnable()) {
//        TODO: do we have to check if any of the elements on the Schedule are invalid? such as the cron?
        workflowScheduleRepository.saveSchedule(entity);
        if (WorkflowScheduleStatus.active.equals(entity.getStatus())) {
          scheduleWorkflow(entity, previous, current);
        }
      }
//      TODO: return an error stating the Scheduler Trigger is not enabled.
        
    }

      String timezone = scheduler.getTimezone();

      if (timezone == null) {
        scheduler.setTimezone(currentTimezone);
      }

      entity.getTriggers().setScheduler(scheduler);

      if (previousTriggers != null && previousTriggers.getScheduler() != null) {
        previousTriggers.getScheduler().getEnable();
      }

      boolean current = scheduler.getEnable();

      
    }
  }
  
  public void deleteAllSchedules(String workflowId) {
    final List<WorkflowScheduleEntity> entities = workflowScheduleRepository.getSchedulesForWorkflow(workflowId);
    if (entities != null) {
      entities.forEach(e -> {
        deleteSchedule(e.getId(), Optional.of(e));
      });
    }
  }
  
  public void deleteSchedule(String scheduleId, Optional<WorkflowScheduleEntity> optional) {
    try {
      this.taskScheduler.cancelJob(scheduleId);
      if (optional.isEmpty()) {
        optional = Optional.ofNullable(workflowScheduleRepository.getSchedule(scheduleId));
      }
      if (optional.isPresent()) {
        optional.get().setStatus(WorkflowScheduleStatus.deleted);
        workflowScheduleRepository.saveSchedule(optional.get());
      }
    } catch (SchedulerException e) {
      logger.info("Unable to remove job. ");
      logger.error(e);
    }
  }
}
