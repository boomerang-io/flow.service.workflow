package io.boomerang.service.refactor;

import io.boomerang.model.Task;

public interface LockManager {
  
  public void acquireLock(Task taskExecution, String activityId);
  public void releaseLock(Task taskExecution, String activityId);

}
