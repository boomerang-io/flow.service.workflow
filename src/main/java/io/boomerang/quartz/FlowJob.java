package io.boomerang.quartz;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.quartz.DisallowConcurrentExecution;
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
@DisallowConcurrentExecution
public class FlowJob extends QuartzJobBean {

  private static final Logger LOG = LoggerFactory.getLogger(FlowJob.class);

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
    LOG.info("This is the FlowJob, executed for {} with JobDetails = {}",
        jobDetail.getKey().getName(), jobDetail.getJobDataMap());

    ExecutionController executionController = applicationContext.getBean(ExecutionController.class);

    String workflowId = jobDetail.getKey().getName();
    
    Map<String, String> properties = new HashMap<>();

    FlowExecutionRequest request = new FlowExecutionRequest();
    request.setProperties(properties);

    executionController.executeWorkflow(workflowId, Optional.of(FlowTriggerEnum.scheduler.toString()), Optional.of(request));
  }


}