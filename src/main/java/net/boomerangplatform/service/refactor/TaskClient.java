package net.boomerangplatform.service.refactor;

import org.springframework.web.bind.annotation.RequestBody;
import net.boomerangplatform.model.RequestFlowExecution;
import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;

public interface TaskClient {
  
  public void startTask(TaskService taskService, InternalTaskRequest taskRequest);  
  public void endTask(TaskService taskService, InternalTaskResponse taskResponse);
  public String submitWebhookEvent(@RequestBody RequestFlowExecution request);
}
