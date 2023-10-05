package io.boomerang.quartz;

import java.util.ArrayList;
import java.util.List;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.model.enums.WorkflowScheduleType;
import io.boomerang.model.ref.WorkflowRunSubmitRequest;
import io.boomerang.security.model.Token;
import io.boomerang.security.service.TokenServiceImpl;
import io.boomerang.service.ScheduleServiceImpl;
import io.boomerang.service.WorkflowRunServiceImpl;

/*
 * This is used by the Quartz Trigger to execute the Scheduled Job
 * 
 * Caution: if this is renamed or moved packages then all the jobs in the DB will need to have the
 * jobClass reference updated.
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

    WorkflowRunServiceImpl workflowRunService =
        applicationContext.getBean(WorkflowRunServiceImpl.class);
    ScheduleServiceImpl workflowScheduleService =
        applicationContext.getBean(ScheduleServiceImpl.class);
    TokenServiceImpl tokenService = applicationContext.getBean(TokenServiceImpl.class);

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

      //Hoist token to ThreadLocal SecurityContext - this AuthN/AuthZ allows the WorkflowRun to be triggered
      Token token = tokenService.createWorkflowSessionToken(jobDetail.getKey().getGroup());
      final List<GrantedAuthority> authorities = new ArrayList<>();
      final UsernamePasswordAuthenticationToken authToken =
          new UsernamePasswordAuthenticationToken(jobDetail.getKey().getGroup(), null, authorities);
      authToken.setDetails(token);
      SecurityContextHolder.getContext().setAuthentication(authToken);
      
      //Auto start is not needed when using the default handler
      //As the default handler will pick up the queued Workflow and start the Workflow when ready.
      //However if using the non-default Handler then this may be needed to be set to true.
      boolean autoStart = applicationContext.getEnvironment().getProperty("flow.workflowrun.auto-start-on-submit", boolean.class);
      workflowRunService.submit(request, autoStart);
    }
  }
}
