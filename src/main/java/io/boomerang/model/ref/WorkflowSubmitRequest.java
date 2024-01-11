package io.boomerang.model.ref;

import io.boomerang.model.enums.TriggerEnum;

/*
 * Extended WorkflowRunSubmitRequest version for the Workflow service that includes triggerDetails
 */
public class WorkflowSubmitRequest extends WorkflowRunRequest {

  private Integer workflowVersion;

  private TriggerEnum trigger;

  public Integer getWorkflowVersion() {
    return workflowVersion;
  }

  public void setWorkflowVersion(Integer workflowVersion) {
    this.workflowVersion = workflowVersion;
  }

  public TriggerEnum getTrigger() {
    return trigger;
  }

  public void setTrigger(TriggerEnum trigger) {
    this.trigger = trigger;
  }
}
