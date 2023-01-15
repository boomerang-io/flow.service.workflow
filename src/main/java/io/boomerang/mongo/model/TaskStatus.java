package io.boomerang.mongo.model;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {

  // @formatter:off
  completed("completed"),
  failure("failure"),
  inProgress("inProgress"),
  notstarted("notstarted"),
  invalid("invalid"),
  skipped("skipped"),
  waiting("waiting"),
  cancelled("cancelled");
  // @formatter:on

  private String status;

  TaskStatus(String status) {
    this.status = status;
  }

  @JsonValue
  public String getStatus() {
    return status;
  }

  public static TaskStatus getFlowTaskStatus(String flowTaskStatus) {
    return Arrays.asList(TaskStatus.values()).stream()
        .filter(value -> value.getStatus().equals(flowTaskStatus)).findFirst().orElse(null);
  }
}
