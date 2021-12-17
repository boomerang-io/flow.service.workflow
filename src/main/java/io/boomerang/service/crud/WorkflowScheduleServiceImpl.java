package io.boomerang.service.crud;


import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.mongo.entity.WorkflowEntity;
import io.boomerang.mongo.entity.WorkflowScheduleEntity;
import io.boomerang.mongo.model.KeyValuePair;
import io.boomerang.mongo.model.WorkflowScheduleStatus;
import io.boomerang.mongo.model.WorkflowScheduleType;
import io.boomerang.mongo.service.FlowWorkflowScheduleService;
import io.boomerang.mongo.service.FlowWorkflowService;
import io.boomerang.quartz.QuartzSchedulerService;
import io.boomerang.util.ParameterMapper;

@Service
public class WorkflowScheduleServiceImpl implements WorkflowScheduleService {

  private final Logger logger = LogManager.getLogger(getClass());

  @Autowired
  private QuartzSchedulerService taskScheduler;

  @Autowired
  private FlowWorkflowScheduleService workflowScheduleRepository;

  @Autowired
  private FlowWorkflowService workflowRepository;
  
  @Override
  public List<WorkflowSchedule> getSchedules(String workflowId) {
    List<WorkflowSchedule> schedules = new LinkedList<>();
    final List<WorkflowScheduleEntity> entities = workflowScheduleRepository.getSchedulesForWorkflow(workflowId);
    if (entities != null) {
      entities.forEach(e -> {
        schedules.add(new WorkflowSchedule(e));
      });
    }
    return schedules;
  }
  
  @Override
  public WorkflowSchedule getSchedule(String workflowId, String scheduleId) {
    final WorkflowScheduleEntity scheduleEntity = workflowScheduleRepository.getSchedule(scheduleId);
    if (scheduleEntity != null && workflowId.equals(scheduleEntity.getWorkflowId())) {
      return new WorkflowSchedule(scheduleEntity);
    }
    return null;
  }
  
  @Override
  public List<Date> getSchedulesForDates(final String workflowId, Date fromDate, Date toDate) {
    List<Date> scheduleDates = new LinkedList<>();
    final List<WorkflowScheduleEntity> scheduleEntities = workflowScheduleRepository.getSchedulesForWorkflow(workflowId);
    if (scheduleEntities != null) {
      scheduleEntities.forEach(e -> {
        scheduleDates.addAll(getScheduleForDates(workflowId, e.getId(), fromDate, toDate));
      });
      return scheduleDates;
    }
    return null;
  }
  
  @Override
  public List<Date> getScheduleForDates(final String workflowId, final String scheduleId, Date fromDate, Date toDate) {
    final WorkflowScheduleEntity scheduleEntity = workflowScheduleRepository.getSchedule(scheduleId);
    if (scheduleEntity != null && workflowId.equals(scheduleEntity.getWorkflowId())) {
      try {
        return this.taskScheduler.getJobTriggerDates(scheduleEntity, fromDate, toDate);
      } catch (SchedulerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
    return null;
  }
  
  @Override
  public WorkflowSchedule createSchedule(final String workflowId, final WorkflowSchedule schedule) {
//  TODO: do we have to check if they have authorization to create a workflow against that team?
//  TODO: do we have to check if any of the elements on the Schedule are invalid? such as the cron?
//  TODO: return an error if the minimum required fields arent provided.

    if (schedule != null && schedule.getWorkflowId() != null && workflowId.equals(schedule.getWorkflowId())) {
      WorkflowEntity wfEntity = workflowRepository.getWorkflow(schedule.getWorkflowId());
      if (wfEntity != null) {
          schedule.setCreationDate(new Date());
          WorkflowScheduleEntity scheduleEntity = new WorkflowScheduleEntity();
          BeanUtils.copyProperties(schedule, scheduleEntity);
          if (schedule.getParametersMap() != null && !schedule.getParametersMap().isEmpty()) {
            List<KeyValuePair> propertyList = ParameterMapper.mapToKeyValuePairList(schedule.getParametersMap());
            scheduleEntity.setParameters(propertyList);
          }
          workflowScheduleRepository.saveSchedule(scheduleEntity);
          Boolean enableJob = false;
          if (WorkflowScheduleStatus.active.equals(schedule.getStatus()) && wfEntity.getTriggers().getScheduler().getEnable()) {
            enableJob = true;
          }
          createOrUpdateSchedule(scheduleEntity, enableJob);
          return new WorkflowSchedule(scheduleEntity);
        }
      }
    return null;
  }
  
  @Override
  public WorkflowSchedule updateSchedule(final String workflowId, final String scheduleId, final WorkflowSchedule patchSchedule) {
    if (patchSchedule != null) {
      WorkflowEntity wfEntity = workflowRepository.getWorkflow(workflowId);
      if (wfEntity != null) {
        WorkflowScheduleEntity scheduleEntity = workflowScheduleRepository.getSchedule(scheduleId);
        if (scheduleEntity != null) {
          BeanUtils.copyProperties(patchSchedule, scheduleEntity, "id", "workflowId", "creationDate", "parameters", "parametersMap");
          if (patchSchedule.getParametersMap() != null && !patchSchedule.getParametersMap().isEmpty()) {
            List<KeyValuePair> propertyList = ParameterMapper.mapToKeyValuePairList(patchSchedule.getParametersMap());
            scheduleEntity.setParameters(propertyList);
          }
          workflowScheduleRepository.saveSchedule(scheduleEntity);
          Boolean enableJob = false;
          if (WorkflowScheduleStatus.active.equals(scheduleEntity.getStatus()) && wfEntity.getTriggers().getScheduler().getEnable()) {
            enableJob = true;
          }
          createOrUpdateSchedule(scheduleEntity, enableJob);
          return new WorkflowSchedule(scheduleEntity);
        }
      }        
    }
    return null;
  }

  private void createOrUpdateSchedule(final WorkflowScheduleEntity schedule, Boolean enableJob) {
    try {
      if (WorkflowScheduleType.runOnce.equals(schedule.getType())) {
        this.taskScheduler.createOrUpdateRunOnceJob(schedule);
      } else {
        this.taskScheduler.createOrUpdateCronJob(schedule);
      }
      if (!enableJob) {
        this.taskScheduler.pauseJob(schedule);
      }
    } catch (SchedulerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  @Override
  public void enableSchedule(String scheduleId) throws SchedulerException {
    WorkflowScheduleEntity schedule = workflowScheduleRepository.getSchedule(scheduleId);
    if (schedule!= null && !WorkflowScheduleStatus.deleted.equals(schedule.getStatus())) {
      schedule.setStatus(WorkflowScheduleStatus.active);
      workflowScheduleRepository.saveSchedule(schedule);
      this.taskScheduler.resumeJob(schedule);
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
      this.taskScheduler.pauseJob(schedule);
    } else {
//        TODO: return that it couldn't be disabled or doesn't exist
    }
  }
  
  @Override
  public void deleteAllSchedules(final String workflowId) {
//    TODO: get this integrated with the deleteWorkflow
    final List<WorkflowScheduleEntity> entities = workflowScheduleRepository.getSchedulesForWorkflow(workflowId);
    if (entities != null) {
      entities.forEach(s -> {
        deleteSchedule(workflowId, s.getId());
      });
    }
  }
  
  @Override
  public ResponseEntity<?> deleteSchedule(final String workflowId, final String scheduleId) {
    try {
      WorkflowScheduleEntity scheduleEntity = workflowScheduleRepository.getSchedule(scheduleId);
      if (scheduleEntity != null && workflowId.equals(scheduleEntity.getWorkflowId())) {
        scheduleEntity.setStatus(WorkflowScheduleStatus.deleted);
        workflowScheduleRepository.saveSchedule(scheduleEntity);
        this.taskScheduler.cancelJob(scheduleEntity);
      } else {
        return ResponseEntity.badRequest().build();
      }
    } catch (SchedulerException e) {
      logger.info("Unable to delete schedule {}.", scheduleId);
      logger.error(e);
      return ResponseEntity.internalServerError().build();
    }
    return ResponseEntity.ok().build();
  }
  
  @Override
  public CronValidationResponse validateCron(String cronString) {

    logger.info("CRON: {}", cronString);

    CronValidationResponse response = new CronValidationResponse();
    CronParser parser =
        new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
    try {
      cronString = parser.parse(cronString).asString();
      response.setCron(cronString);
      response.setValid(true);
      logger.info("Final CRON: {} .", cronString);
    } catch (IllegalArgumentException e) {
      logger.info("Invalid CRON: {} . Attempting cron to quartz conversion", cronString);
      parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.CRON4J));
      try {
        Cron cron = parser.parse(cronString);
        CronMapper quartzMapper = CronMapper.fromCron4jToQuartz();
        Cron quartzCron = quartzMapper.map(cron);
        cronString = quartzCron.asString();
        response.setCron(cronString);
        response.setValid(true);
      } catch (IllegalArgumentException exc) {
        logger.info("Invalid CRON: {} . Cannot convert", cronString);
        response.setCron(null);
        response.setValid(false);
        response.setMessage(e.getMessage());
      }

      logger.info("Final CRON: {} .", cronString);
    }
    return response;
  }
}
