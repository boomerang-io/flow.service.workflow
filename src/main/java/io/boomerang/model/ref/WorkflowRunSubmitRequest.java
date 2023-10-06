package io.boomerang.model.ref;

/*
 * Extended WorkflowRunSubmitRequest version for the Workflow service that includes eventType and
 * eventSource trigger details
 */
public class WorkflowRunSubmitRequest extends WorkflowRunRequest {

  private String workflowRef;

  private Integer workflowVersion;

  private String trigger;

  private String eventType;

  private String eventSubject;

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

  public String getTrigger() {
    return trigger;
  }

  public void setTrigger(String trigger) {
    this.trigger = trigger;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(String eventType) {
    this.eventType = eventType;
  }

  public String getEventSubject() {
    return eventSubject;
  }

  public void setEventSubject(String eventSubject) {
    this.eventSubject = eventSubject;
  }
}
