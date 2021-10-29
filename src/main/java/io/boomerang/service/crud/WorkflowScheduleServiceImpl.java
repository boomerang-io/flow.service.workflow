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
import io.boomerang.quartz.ScheduledTasks;

public class WorkflowScheduleServiceImpl {

  private final Logger logger = LogManager.getLogger(getClass());

  @Autowired
  private ScheduledTasks taskScheduler;

  @Autowired
  private FlowWorkflowScheduleService workflowScheduleRepository;

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
  
  private void updateSchedule(final WorkflowEntity entity, Triggers previousTriggers,
      String currentTimezone, boolean previous, TriggerScheduler scheduler) {
    if (scheduler != null) {

      String timezone = scheduler.getTimezone();

      if (timezone == null) {
        scheduler.setTimezone(currentTimezone);
      }

      entity.getTriggers().setScheduler(scheduler);

      if (previousTriggers != null && previousTriggers.getScheduler() != null) {
        previousTriggers.getScheduler().getEnable();
      }

      boolean current = scheduler.getEnable();

      scheduleWorkflow(entity, previous, current);
    }
  }
  
  public void deleteAllSchedules(String workflowId) {
    final List<WorkflowScheduleEntity> entities = workflowScheduleRepository.getSchedulesForWorkflow(workflowId);
    if (entities != null) {
      entities.forEach(e -> {
        deleteSchedule(e.getId(), e);
      });
    }
  }
  
  public void deleteSchedule(String scheduleId, WorkflowScheduleEntity entity) {
    try {
      this.taskScheduler.cancelJob(scheduleId);
      if (entity.isEmpty()) {
        entity = Optional.ofNullable(workflowScheduleRepository.getSchedule(scheduleId));
      }
      if (entity.isPresent()) {
      entity.get().setStatus(WorkflowScheduleStatus.deleted);
      workflowScheduleRepository.saveSchedule(entity.get());
      }
    } catch (SchedulerException e) {
      logger.info("Unable to remove job. ");
      logger.error(e);
    }
  }
}
