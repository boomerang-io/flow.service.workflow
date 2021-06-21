package net.boomerangplatform.quartz;

import java.util.List;
import java.util.TimeZone;
import java.util.function.Predicate;
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
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;
import net.boomerangplatform.mongo.entity.WorkflowEntity;
import net.boomerangplatform.mongo.service.FlowWorkflowService;

@Component
@ConditionalOnProperty(
    value="flow.scheduling.enabled", 
    havingValue = "true", 
    matchIfMissing = true)
public class ScheduledTasks {

  private final Logger logger = LogManager.getLogger(getClass());

  @Autowired
  private SchedulerFactoryBean schedulerFactoryBean;

  @Autowired
  private FlowWorkflowService flowWorkflowService;


  public void cancelJob(String workflowId) throws SchedulerException {
    List<WorkflowEntity> scheduledWorkflows = flowWorkflowService.getScheduledWorkflows();
    Scheduler scheduler = schedulerFactoryBean.getScheduler();
    Predicate<WorkflowEntity> p1 = e -> workflowId.equals(e.getId());
    boolean exists = scheduledWorkflows.stream().anyMatch(p1);
    JobKey jobkey = new JobKey(workflowId, "flow");

    if (exists) {
      logger.info("Removing job no longer needed: {}", workflowId);
      boolean deleted = scheduler.deleteJob(jobkey);
      logger.info("Deleted: {}", deleted);
    }
  }

 
  public void scheduleWorkflow(WorkflowEntity workflow) {
    
    if (workflow.getTriggers() != null && workflow.getTriggers().getScheduler() != null) {
      
      String cronString = workflow.getTriggers().getScheduler().getSchedule();
      String timezone = workflow.getTriggers().getScheduler().getTimezone();
      if (cronString != null && timezone != null) {
        boolean validCron = org.quartz.CronExpression.isValidExpression(cronString);
        if (!validCron) {
          logger.info("Invalid CRON: {}", cronString);
          return;
        }
        TimeZone timeZone = TimeZone.getTimeZone(timezone);
        String workflowId = workflow.getId();
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        JobDetail jobDetail =
            JobBuilder.newJob(FlowJob.class).withIdentity(workflowId, "flow").build();
       
        try {
          if (!scheduler.checkExists(jobDetail.getKey())) {
            CronScheduleBuilder scheduleBuilder =
                CronScheduleBuilder.cronSchedule(cronString).inTimeZone(timeZone);

            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(workflowId, "flow")
                .withSchedule(scheduleBuilder).build();
            try {
              scheduler.scheduleJob(jobDetail, trigger);
              logger.info("Scheduled Workflow: {}", workflowId);
            } catch (SchedulerException e) {
              logger.error(e);
            }
          }
        } catch (SchedulerException e1) {
          logger.error("Unable to schedule workflow");
          logger.error(ExceptionUtils.getStackTrace(e1));
        }
      }
    }
  }
}
