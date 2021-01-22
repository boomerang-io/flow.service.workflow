package net.boomerangplatform.service.refactor;

import net.boomerangplatform.mongo.entity.TaskExecutionEntity;

public interface LockManager {
  
  public void acquireLock(TaskExecutionEntity taskExecution);
  public void releaseLock(TaskExecutionEntity taskExecution);

}
