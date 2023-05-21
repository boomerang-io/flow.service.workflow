package io.boomerang.v4.model.ref;

import java.util.Map;

public class WorkflowRunCount {

  private Map<String, Long> status;

  public Map<String, Long> getStatus() {
    return status;
  }

  public void setStatus(Map<String, Long> status) {
    this.status = status;
  }
}
