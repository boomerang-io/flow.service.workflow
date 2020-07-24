package net.boomerangplatform.model;

import net.boomerangplatform.mongo.model.FlowTaskStatus;

public class TaskResult {

  private String node;
  private FlowTaskStatus status;

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public FlowTaskStatus getStatus() {
    return status;
  }

  public void setStatus(FlowTaskStatus status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return "node: " + node + " - " + status.toString();
  }
}
