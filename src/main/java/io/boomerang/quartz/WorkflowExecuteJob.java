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
import io.boomerang.model.WorkflowSchedule;
import io.boomerang.mongo.model.FlowTriggerEnum;
import io.boomerang.mongo.model.WorkflowScheduleType;
import io.boomerang.service.crud.WorkflowScheduleService;
import io.boomerang.util.ParameterMapper;

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
    WorkflowScheduleService workflowScheduleService = applicationContext.getBean(WorkflowScheduleService.class);

    String workflowId = jobDetail.getKey().getGroup();
    
    Map<String, String> properties = new HashMap<>();
    
    WorkflowSchedule schedule = workflowScheduleService.getSchedule(jobDetail.getKey().getName());
    if (schedule != null) {
      if (schedule.getParameters() != null) {
        properties = ParameterMapper.keyValuePairListToMap(schedule.getParameters());
      }
      if (schedule.getType().equals(WorkflowScheduleType.runOnce)) {
        logger.info("Executing runOnce schedule: {}, and marking as deleted.", schedule.getId());
        workflowScheduleService.deleteSchedule(schedule.getId());
        //TODO: confirm if we delete or mark completed
      }
    }

    FlowExecutionRequest request = new FlowExecutionRequest();
    request.setProperties(properties);

    executionController.executeWorkflow(workflowId, Optional.of(FlowTriggerEnum.scheduler.toString()), Optional.of(request));
  }
}