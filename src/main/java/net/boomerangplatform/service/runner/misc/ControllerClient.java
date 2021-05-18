package net.boomerangplatform.service.runner.misc;

import java.util.List;
import java.util.Map;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.mongo.model.CoreProperty;

public interface ControllerClient {
  
  public void terminateTask(Task task);
  
  public void submitTemplateTask(Task task, String activityId, String workflowName, List<CoreProperty> labels);
  
  public void submitCustomTask(Task task, String activityId, String workflowName, List<CoreProperty> labels);

  boolean terminateFlow(String workflowId, String workflowName, String activityId);

  boolean createFlow(String workflowId, String workflowName, String activityId,
      boolean enableStorage,  List<CoreProperty> labels, Map<String, String> properties);
}
