package net.boomerangplatform.service.runner.misc;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import net.boomerangplatform.model.Task;
import net.boomerangplatform.mongo.model.TaskStatus;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;
import net.boomerangplatform.service.refactor.TaskClient;

@Service
@Profile("local")
public class MockControllerClient implements ControllerClient {
  
  @Autowired
  private TaskClient flowTaskClient;
  
  @Override
  public void submitTemplateTask(Task task, String activityId, String workflowName) {
    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(TaskStatus.completed);
    flowTaskClient.endTask(response);
    return;
  }

  @Override
  public void submitCustomTask(Task task, String activityId, String workflowName) {
    InternalTaskResponse response = new InternalTaskResponse();
    response.setActivityId(task.getTaskActivityId());
    response.setStatus(TaskStatus.completed);
    flowTaskClient.endTask(response);
    return;
  }

  @Override
  public boolean terminateFlow(String workflowId, String workflowName, String activityId) {
    return true;
  }

  @Override
  public boolean createFlow(String workflowId, String workflowName, String activityId,
      boolean enableStorage, Map<String, String> properties) {
    return true;
  }
}
