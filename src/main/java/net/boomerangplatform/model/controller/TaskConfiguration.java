package net.boomerangplatform.model.controller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties
public class TaskConfiguration {

  private Boolean debug;

  private Boolean lifecycle;

  private TaskDeletion deletion;

  public Boolean getDebug() {
    return debug;
  }

  public void setDebug(Boolean debug) {
    this.debug = debug;
  }

  public Boolean getLifecycle() {
    return lifecycle;
  }

  public void setLifecycle(Boolean lifecycle) {
    this.lifecycle = lifecycle;
  }

  public TaskDeletion getDeletion() {
    return deletion;
  }

  public void setDeletion(TaskDeletion deletion) {
    this.deletion = deletion;
  }

}
