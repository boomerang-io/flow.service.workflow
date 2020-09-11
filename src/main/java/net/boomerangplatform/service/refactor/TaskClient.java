package net.boomerangplatform.service.refactor;

import net.boomerangplatform.mongo.model.internal.InternalTaskRequest;
import net.boomerangplatform.mongo.model.internal.InternalTaskResponse;

public interface TaskClient {
  
  public void startTask(InternalTaskRequest taskRequest);  
  public void endTask(InternalTaskResponse taskResponse);

}
