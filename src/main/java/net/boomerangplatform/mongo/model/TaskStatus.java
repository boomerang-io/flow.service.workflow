package net.boomerangplatform.mongo.model;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskStatus {
  completed("completed"), failure("failure"), inProgress("inProgress"), notstarted( // NOSONAR
      "notstarted"), invalid("invalid"), skipped("skipped"), waiting("waiting"); // NOSONAR

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
