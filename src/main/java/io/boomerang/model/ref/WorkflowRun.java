package io.boomerang.model.ref;

import java.util.List;
import org.springframework.beans.BeanUtils;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.boomerang.data.entity.ref.WorkflowRunEntity;

@JsonPropertyOrder({"id", "creationDate", "status", "phase", "startTime", "duration", "statusMessage", "error", "timeout", "retries", "workflowRef", "workflowRevisionRef", "labels", "annotations", "params", "tasks" })
public class WorkflowRun extends WorkflowRunEntity {

  private List<TaskRun> tasks;
  
  private String workflowName;
  
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

  public String getWorkflowName() {
    return workflowName;
  }

  public void setWorkflowName(String workflowName) {
    this.workflowName = workflowName;
  }
}