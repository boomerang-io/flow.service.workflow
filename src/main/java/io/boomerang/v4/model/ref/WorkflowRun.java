package io.boomerang.v4.model.ref;

import java.util.List;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.v4.data.entity.ref.WorkflowRunEntity;

@JsonPropertyOrder({"id", "creationDate", "status", "phase", "startTime", "duration", "statusMessage", "error", "timeout", "retries", "workflowRef", "workflowRevisionRef", "labels", "annotations", "params", "tasks" })
public class WorkflowRun extends WorkflowRunEntity {

  private List<TaskRun> tasks;
  
  public WorkflowRun() {
    
  }

  public WorkflowRun(WorkflowRunEntity entity) {
    BeanUtils.copyProperties(entity, this, "debug", "taskDeletion");
  }

  @Override
  public String toString() {
    return "WorkflowRun [tasks=" + tasks + ", toString()=" + super.toString() + "]";
  }

  public List<TaskRun> getTasks() {
    return tasks;
  }

  public void setTasks(List<TaskRun> taskRuns) {
    this.tasks = taskRuns;
  }
}
