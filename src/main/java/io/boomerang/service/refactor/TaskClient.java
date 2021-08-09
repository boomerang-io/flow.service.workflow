package io.boomerang.service.refactor;

import org.springframework.web.bind.annotation.RequestBody;
import io.boomerang.model.RequestFlowExecution;
import io.boomerang.mongo.model.internal.InternalTaskRequest;
import io.boomerang.mongo.model.internal.InternalTaskResponse;

public interface TaskClient {
  
  public void startTask(TaskService taskService, InternalTaskRequest taskRequest);  
  public void endTask(TaskService taskService, InternalTaskResponse taskResponse);
  public String submitWebhookEvent(@RequestBody RequestFlowExecution request);
}
