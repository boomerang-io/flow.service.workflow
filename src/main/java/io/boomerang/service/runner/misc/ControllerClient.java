package io.boomerang.service.runner.misc;

import java.util.List;
import java.util.Map;
import io.boomerang.model.Task;
import io.boomerang.mongo.model.CoreProperty;
import io.boomerang.service.refactor.TaskClient;
import io.boomerang.service.refactor.TaskService;

public interface ControllerClient {
  
  public void terminateTask(Task task);
  
  public void submitTemplateTask(TaskService taskService, TaskClient flowTaskClient,Task task, String activityId, String workflowName, List<CoreProperty> labels);
  
  public void submitCustomTask(TaskService taskService, TaskClient flowTaskClient, Task task, String activityId, String workflowName, List<CoreProperty> labels);

  boolean terminateFlow(String workflowId, String workflowName, String activityId);

  boolean createFlow(String workflowId, String workflowName, String activityId,
      boolean enableStorage,  List<CoreProperty> labels, Map<String, String> properties);
}
