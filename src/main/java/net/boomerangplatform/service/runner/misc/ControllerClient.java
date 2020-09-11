package net.boomerangplatform.service.runner.misc;

import java.util.Map;
import net.boomerangplatform.model.Task;

public interface ControllerClient {
  
  public void submitTemplateTask(Task task, String activityId, String workflowName);
  
  public void submitCustomTask(Task task, String activityId, String workflowName);

  boolean terminateFlow(String workflowId, String workflowName, String activityId);

  boolean createFlow(String workflowId, String workflowName, String activityId,
      boolean enableStorage, Map<String, String> properties);
}
