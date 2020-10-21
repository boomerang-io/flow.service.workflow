package net.boomerangplatform.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import net.boomerangplatform.controller.ExecutionController;
import net.boomerangplatform.model.FlowExecutionRequest;
import net.boomerangplatform.mongo.model.FlowTriggerEnum;

public class FlowJob implements Job {

  @Autowired
  private ExecutionController controller;

  @Override
  public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

    Map<String, String> properties = new HashMap<>();

    FlowExecutionRequest request = new FlowExecutionRequest();
    request.setProperties(properties);

    String workflowId = jobExecutionContext.getJobDetail().getKey().getName();
    controller.executeWorkflow(workflowId, Optional.of(FlowTriggerEnum.scheduler.toString()), Optional.of(request));
  }
}
