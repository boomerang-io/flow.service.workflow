package net.boomerangplatform.service.refactor;

import net.boomerangplatform.model.Task;

public interface LockManager {
  
  public void acquireLock(Task taskExecution, String activityId);
  public void releaseLock(Task taskExecution, String activityId);

}
