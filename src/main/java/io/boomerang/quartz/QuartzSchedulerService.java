package io.boomerang.quartz;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.calendar.BaseCalendar;
import org.quartz.spi.OperableTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import io.boomerang.data.entity.WorkflowScheduleEntity;
import io.boomerang.model.CronValidationResponse;
import io.boomerang.service.ScheduleService;

@Component
public class QuartzSchedulerService {

  private final Logger logger = LogManager.getLogger(getClass());

  @Autowired
  private SchedulerFactoryBean schedulerFactoryBean;

  @Autowired
  private ScheduleService workflowScheduleService;

  public void createOrUpdateCronJob(String team, WorkflowScheduleEntity schedule) {
    String cronString = schedule.getCronSchedule();
    String timezone = schedule.getTimezone();
    if (cronString != null && timezone != null) {
      boolean validCron = org.quartz.CronExpression.isValidExpression(cronString);
      if (!validCron) {
        logger.info("Invalid CRON: {}. Attempting to convert.", cronString);
        CronValidationResponse response = workflowScheduleService.validateCron(cronString);
        if (response.isValid()) {
          cronString = response.getCron();
          logger.info("CRON converted: {}.", cronString);
        } else {
          logger.info("Invalid CRON: {}.", cronString);
          return;
        }
      }
      TimeZone timeZone = TimeZone.getTimeZone(timezone);
      String scheduleId = schedule.getId();
      String workflowId = schedule.getWorkflowRef();
      Scheduler scheduler = schedulerFactoryBean.getScheduler();
      JobDetail jobDetail =
          JobBuilder.newJob(QuartzSchedulerJob.class).withIdentity(scheduleId, workflowId).withDescription(team).build();         
//    TODO: determine if we add a calendar entry for excluded dates
    CronScheduleBuilder cronScheduleBuilder =
        CronScheduleBuilder.cronSchedule(cronString).inTimeZone(timeZone);
    CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(scheduleId, workflowId)
        .withSchedule(cronScheduleBuilder).build();
    
      try {
        if (!scheduler.checkExists(jobDetail.getKey())) {
          scheduler.scheduleJob(jobDetail, trigger);
          logger.info("Scheduled Cron Schedule: {} for Workflow: {}.", scheduleId, workflowId);
        } else {
          scheduler.rescheduleJob(new TriggerKey(schedule.getId(), schedule.getWorkflowRef()),trigger);
          logger.info("Updated RunOnce Schedule: {} for Workflow: {}.", scheduleId, workflowId);
        }
      } catch (SchedulerException e1) {
        logger.error("Unable to schedule workflow");
        logger.error(ExceptionUtils.getStackTrace(e1));
      }
    }
  }
  
  public void createOrUpdateRunOnceJob(String team, WorkflowScheduleEntity schedule) throws Exception {
    String scheduleId = schedule.getId();
    String workflowId = schedule.getWorkflowRef();
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    JobDetail jobDetail = JobBuilder.newJob(QuartzSchedulerJob.class).withIdentity(scheduleId, workflowId).withDescription(team).build();
    SimpleScheduleBuilder simpleScheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withRepeatCount(0);
    SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity(scheduleId, workflowId).startAt(schedule.getDateSchedule())
        .withSchedule(simpleScheduleBuilder).build();
    try {
      if (!scheduler.checkExists(jobDetail.getKey())) {
          scheduler.scheduleJob(jobDetail, trigger);
          logger.info("Created RunOnce Schedule: {} for Workflow: {}.", scheduleId, workflowId);
      } else {
        scheduler.rescheduleJob(new TriggerKey(schedule.getId(), schedule.getWorkflowRef()),trigger);
        logger.info("Updated RunOnce Schedule: {} for Workflow: {}.", scheduleId, workflowId);
      }
    } catch (SchedulerException e1) {
      logger.error("Unable to create or update scheduled job");
      logger.error(ExceptionUtils.getStackTrace(e1));
    }
  }
  
  public void pauseJob(WorkflowScheduleEntity schedule) throws SchedulerException {
    logger.debug("Pause Job - " + schedule.getId() + " " + schedule.getWorkflowRef());
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    scheduler.pauseJob(new JobKey(schedule.getId(), schedule.getWorkflowRef()));
  }
  
  public void resumeJob(WorkflowScheduleEntity schedule) throws SchedulerException {
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    scheduler.resumeJob(new JobKey(schedule.getId(), schedule.getWorkflowRef()));
  }
  
  public void cancelJob(WorkflowScheduleEntity schedule) throws SchedulerException {
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    scheduler.deleteJob(new JobKey(schedule.getId(), schedule.getWorkflowRef()));
  }
  
  public List<Date> getJobTriggerDates(WorkflowScheduleEntity schedule, Date fromDate, Date toDate) throws SchedulerException {
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    Trigger trigger = scheduler.getTrigger(new TriggerKey(schedule.getId(), schedule.getWorkflowRef()));
    if (trigger != null) {
      logger.info("Retrieving Dates from: " + fromDate.toString() + ", to: " + toDate.toString() + ", for Schedule: " + schedule.getId());
      return org.quartz.TriggerUtils.computeFireTimesBetween((OperableTrigger) trigger, new BaseCalendar(), fromDate, toDate);
    }
    logger.error("Unable to retrieve calendar for Schedule: {}, skipping.", schedule.getId());
    return new LinkedList<Date>();
  }
  
  public Date getNextTriggerDate(WorkflowScheduleEntity schedule) throws SchedulerException {
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    Trigger trigger = scheduler.getTrigger(new TriggerKey(schedule.getId(), schedule.getWorkflowRef()));
    logger.info("Retrieving Next Schedule Date for Schedule: " + schedule.getId());
    return trigger.getNextFireTime();
  }
}
