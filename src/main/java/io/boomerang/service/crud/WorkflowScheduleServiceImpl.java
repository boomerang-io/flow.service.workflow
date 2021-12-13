package io.boomerang.service.crud;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.mongo.model.WorkflowScheduleStatus;
import io.boomerang.mongo.service.FlowWorkflowScheduleService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.quartz.ScheduledTasks;

@Service
public class WorkflowScheduleServiceImpl implements WorkflowScheduleService {

  private final Logger logger = LogManager.getLogger(getClass());

  @Autowired
  private ScheduledTasks taskScheduler;

  @Autowired
  private FlowWorkflowScheduleService workflowScheduleRepository;

  @Autowired
  private FlowWorkflowService workflowRepository;

  private void scheduleWorkflow(final WorkflowScheduleEntity schedule, WorkflowScheduleStatus previousStatus, WorkflowScheduleStatus currentStatus) {
    boolean previous = WorkflowScheduleStatus.active.equals(previousStatus);
    boolean current = WorkflowScheduleStatus.active.equals(currentStatus);
    if (!previous && current) {
      this.taskScheduler.scheduleWorkflow(schedule);
    } else if (previous && !current) {
      try {
        this.taskScheduler.cancelJob(schedule.getId());
      } catch (SchedulerException e) {
        logger.info("Unable to cancel schedule job.");
        logger.error(e);
      }
    } else if (current) {
      try {
        this.taskScheduler.cancelJob(schedule.getId());
        this.taskScheduler.scheduleWorkflow(schedule);

      } catch (SchedulerException e) {
        logger.info("Unable to reschedule job.");
        logger.error(e);
      }
    }
  }
  
  @Override
  public void createSchedule(final WorkflowScheduleEntity schedule) {
//    TODO: do we have to map a model to the entity for external consumption
    if (schedule != null) {
      WorkflowEntity wfEntity = workflowRepository.getWorkflow(schedule.getWorkflowId());
      if (wfEntity != null && wfEntity.getTriggers().getScheduler().getEnable()) {
          //TODO: do we have to check if any of the elements on the Schedule are invalid? such as the cron?
          workflowScheduleRepository.saveSchedule(schedule);
          if (WorkflowScheduleStatus.active.equals(schedule.getStatus())) {
            scheduleWorkflow(schedule, null, schedule.getStatus());
          }
        }
      }
//      TODO: return an error stating the Scheduler Trigger is not enabled.
  }
  
  @Override
  public void updateSchedule(final WorkflowScheduleEntity schedule) {
//    TODO: do we have to map a model to the entity for external consumption
    if (schedule != null) {
      WorkflowEntity wfEntity = workflowRepository.getWorkflow(schedule.getWorkflowId());
      if (wfEntity != null && wfEntity.getTriggers().getScheduler().getEnable()) {
        WorkflowScheduleEntity previousSchedule = workflowScheduleRepository.getSchedule(schedule.getId());
        if (previousSchedule != null) {
          //schedule exists and so we can update it.
          //TODO: do we have to check if any of the elements on the Schedule are invalid? such as the cron?
          workflowScheduleRepository.saveSchedule(schedule);
          if (WorkflowScheduleStatus.active.equals(schedule.getStatus())) {
            scheduleWorkflow(schedule, previousSchedule.getStatus(), schedule.getStatus());
          }
        }
        //TODO: return a failure that the schedule can't be updated because it didn't exist
      }
//      TODO: return an error stating the Scheduler Trigger is not enabled.
        
    }
  }
  
  @Override
  public void enableSchedule(String scheduleId) throws SchedulerException {
    WorkflowScheduleEntity schedule = workflowScheduleRepository.getSchedule(scheduleId);
    if (schedule!= null && !WorkflowScheduleStatus.deleted.equals(schedule.getStatus())) {
      schedule.setStatus(WorkflowScheduleStatus.active);
      workflowScheduleRepository.saveSchedule(schedule);
//        TODO: Schedule the quartz job
    } else {
//        TODO: return that it couldn't be enabled or doesn't exist
    }
  }
  
  @Override
  public void disableSchedule(String scheduleId) throws SchedulerException {
    WorkflowScheduleEntity schedule = workflowScheduleRepository.getSchedule(scheduleId);
    if (schedule!= null && !WorkflowScheduleStatus.deleted.equals(schedule.getStatus())) {
      schedule.setStatus(WorkflowScheduleStatus.inactive);
      workflowScheduleRepository.saveSchedule(schedule);
//        TODO: remove the scheduled quartz job
    } else {
//        TODO: return that it couldn't be disabled or doesn't exist
    }
  }
  
  @Override
  public void deleteAllSchedules(String workflowId) {
    final List<WorkflowScheduleEntity> entities = workflowScheduleRepository.getSchedulesForWorkflow(workflowId);
    if (entities != null) {
      entities.forEach(s -> {
        deleteSchedule(s.getId(), Optional.of(s));
      });
    }
  }
  
  private void deleteSchedule(String scheduleId, Optional<WorkflowScheduleEntity> workflowSchedule) {
    try {
      this.taskScheduler.cancelJob(scheduleId);
      if (workflowSchedule.isEmpty()) {
        workflowSchedule = Optional.ofNullable(workflowScheduleRepository.getSchedule(scheduleId));
      }
      workflowSchedule.get().setStatus(WorkflowScheduleStatus.deleted);
      workflowScheduleRepository.saveSchedule(workflowSchedule.get());
    } catch (SchedulerException e) {
      logger.info("Unable to delete schedule {}.", scheduleId);
      logger.error(e);
    }
  }
}
