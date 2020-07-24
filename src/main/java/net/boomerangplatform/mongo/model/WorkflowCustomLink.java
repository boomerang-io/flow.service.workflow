package net.boomerangplatform.mongo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class WorkflowCustomLink extends WorkflowLink {

  private WorkflowExecutionCondition executionCondition;

  public WorkflowExecutionCondition getExecutionCondition() {
    return executionCondition;
  }

  public void setExecutionCondition(WorkflowExecutionCondition executionCondition) {
    this.executionCondition = executionCondition;
  }

}
