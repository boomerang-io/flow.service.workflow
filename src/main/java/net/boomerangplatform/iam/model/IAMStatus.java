package net.boomerangplatform.iam.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IAMStatus {
  @JsonProperty("Started")
  STARTED, @JsonProperty("Completed")
  COMPLETED, @JsonProperty("Pending")
  PENDING, @JsonProperty("Terminated")
  TERMINATED

}
