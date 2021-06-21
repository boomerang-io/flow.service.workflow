package net.boomerangplatform.model.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug;

  private int timeout;

  private TaskDeletion deletion;

  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public TaskDeletion getDeletion() {
    return deletion;
  }

  public void setDeletion(TaskDeletion deletion) {
    this.deletion = deletion;
  }

}
