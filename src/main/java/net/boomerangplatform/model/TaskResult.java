package net.boomerangplatform.model;

import net.boomerangplatform.mongo.model.TaskStatus;

public class TaskResult {

  private String node;
  private TaskStatus status;

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "node: " + node + " - " + status.toString();
  }
}
