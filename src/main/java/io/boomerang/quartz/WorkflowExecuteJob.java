package io.boomerang.quartz;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import io.boomerang.controller.ExecutionController;
import io.boomerang.model.FlowExecutionRequest;
import io.boomerang.mongo.model.FlowTriggerEnum;

@PersistJobDataAfterExecution
public class WorkflowExecuteJob extends QuartzJobBean {

  private static final Logger logger = LoggerFactory.getLogger(WorkflowExecuteJob.class);

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
    logger.info("This is the Quartz WorkflowExecuteJob, executed for {} with JobDetails = {}",
        jobDetail.getKey().getName(), jobDetail.getJobDataMap());
    
    if (applicationContext == null) {
      logger.info("applicationContext is null");
    }

    ExecutionController executionController = applicationContext.getBean(ExecutionController.class);

    String workflowId = jobDetail.getKey().getGroup();
    
    Map<String, String> properties = new HashMap<>();
    
    
    //TODO: get the workflow parameters off the schedule

    FlowExecutionRequest request = new FlowExecutionRequest();
    request.setProperties(properties);

    executionController.executeWorkflow(workflowId, Optional.of(FlowTriggerEnum.scheduler.toString()), Optional.of(request));
  }
}