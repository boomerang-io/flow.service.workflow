package net.boomerangplatform.mongo.model;

import java.util.Arrays;
import com.fasterxml.jackson.annotation.JsonValue;

public enum FlowTaskStatus {
  completed("completed"), failure("failure"), inProgress("inProgress"), notstarted( // NOSONAR
      "notstarted"), invalid("invalid"), skipped("skipped"); // NOSONAR

  private String status;

  FlowTaskStatus(String status) {
    this.status = status;
  }

  @JsonValue
  public String getStatus() {
    return status;
  }

  public static FlowTaskStatus getFlowTaskStatus(String flowTaskStatus) {
    return Arrays.asList(FlowTaskStatus.values()).stream()
        .filter(value -> value.getStatus().equals(flowTaskStatus)).findFirst().orElse(null);
  }
}
