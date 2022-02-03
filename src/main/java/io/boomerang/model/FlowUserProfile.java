package io.boomerang.model;

import java.util.List;
import io.boomerang.mongo.entity.FlowUserEntity;

public class FlowUserProfile extends FlowUserEntity {
  
  private List<WorkflowSummary> workflows;

  public List<WorkflowSummary> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<WorkflowSummary> workflows) {
    this.workflows = workflows;
  }

}
