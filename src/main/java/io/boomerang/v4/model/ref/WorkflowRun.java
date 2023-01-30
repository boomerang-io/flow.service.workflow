package io.boomerang.v4.model.ref;

import java.util.List;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.v4.data.entity.ref.WorkflowRunEntity;

@JsonPropertyOrder({"id", "creationDate", "status", "phase", "duration", "workflowRef", "workflowRevisionRef", "labels", "params", "tasks" })
public class WorkflowRun extends WorkflowRunEntity {

  private List<TaskRun> tasks;
  
  public WorkflowRun() {
    
  }

  public WorkflowRun(WorkflowRunEntity entity) {
    BeanUtils.copyProperties(entity, this);
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
