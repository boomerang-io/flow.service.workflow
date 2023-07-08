package io.boomerang.quartz;

import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import io.boomerang.service.ScheduleServiceImpl;
import io.boomerang.service.WorkflowRunServiceImpl;
import io.boomerang.v4.model.WorkflowSchedule;
import io.boomerang.v4.model.enums.WorkflowScheduleType;
import io.boomerang.v4.model.ref.WorkflowRunSubmitRequest;

/*
 * This is used by the Quartz Trigger to execute the Scheduled Job
 * 
 * Caution: if this is renamed or moved packages then all the jobs in the DB will need to have the jobClass reference updated.
 */
@PersistJobDataAfterExecution
public class QuartzSchedulerJob extends QuartzJobBean {

  private static final Logger logger = LoggerFactory.getLogger(QuartzSchedulerJob.class);

  private ApplicationContext applicationContext;

  /**
   * This method is called by Spring since we set the
   * {@link SchedulerFactoryBean#setApplicationContextSchedulerContextKey(String)} in
   * {@link QuartzConfiguration}
   * 
   * @param applicationContext
   */
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  /**
   * This is the method that will be executed each time the trigger is fired.
   */
  @Override
  protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
    JobDetail jobDetail = context.getJobDetail();
    logger.info("This is the Quartz Execute Job, executed for {} with JobDetails = {}",
        jobDetail.getKey().getName(), jobDetail.getJobDataMap());
    
    if (applicationContext == null) {
      logger.info("applicationContext is null");
    }

    WorkflowRunServiceImpl workflowRunService = applicationContext.getBean(WorkflowRunServiceImpl.class);
    ScheduleServiceImpl workflowScheduleService = applicationContext.getBean(ScheduleServiceImpl.class);
    
    WorkflowSchedule schedule = workflowScheduleService.internalGet(jobDetail.getKey().getName());
    if (schedule != null) {
      if (schedule.getType().equals(WorkflowScheduleType.runOnce)) {
        logger.info("Executing runOnce schedule: {}, and marking as completed.", schedule.getId());
        workflowScheduleService.complete(schedule.getId());
      }

      WorkflowRunSubmitRequest request = new WorkflowRunSubmitRequest();
      request.setWorkflowRef(jobDetail.getKey().getGroup());
      request.setLabels(schedule.getLabels());
      request.setParams(request.getParams());
      request.setTrigger("schedule");
      
      workflowRunService.internalSubmit(request, true);
    }
    }
}