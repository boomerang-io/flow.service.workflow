//package io.boomerang.v4.client;
//
//import java.util.List;
//import java.util.Map;
//import io.boomerang.model.Task;
//import io.boomerang.mongo.model.KeyValuePair;
//import io.boomerang.service.refactor.TaskClient;
//import io.boomerang.service.refactor.TaskService;
//
//public interface ControllerClient {
//  
//  public void terminateTask(Task task);
//  
//  public void submitTemplateTask(TaskService taskService, TaskClient flowTaskClient,Task task, String activityId, String workflowName, List<KeyValuePair> labels);
//  
//  public void submitCustomTask(TaskService taskService, TaskClient flowTaskClient, Task task, String activityId, String workflowName, List<KeyValuePair> labels);
//
//  boolean terminateFlow(String workflowId, String workflowName, String activityId);
//
//  boolean createFlow(String workflowId, String workflowName, String activityId,
//      boolean enableStorage,  List<KeyValuePair> labels, Map<String, String> properties);
//  
//  public void createWorkspace(String id);
//  
//  public void deleteWorkspace(String id);
//}
