package io.boomerang.model.ref;

import java.util.HashMap;
import java.util.Map;
import io.boomerang.model.enums.TriggerEnum;

/*
 * Extended WorkflowRunSubmitRequest version for the Workflow service that includes triggerDetails
 */
public class WorkflowRunSubmitRequest extends WorkflowRunRequest {

  private String workflowRef;

  private Integer workflowVersion;

  private TriggerEnum trigger;

  private Map<String, Object> triggerDetails = new HashMap<>();

  public String getWorkflowRef() {
    return workflowRef;
  }

  public void setWorkflowRef(String workflowRef) {
    this.workflowRef = workflowRef;
  }

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

  public Map<String, Object> getTriggerDetails() {
    return triggerDetails;
  }

  public void setEventType(Map<String, Object> triggerDetails) {
    this.triggerDetails = triggerDetails;
  }
}
