package io.boomerang.mongo.model;

public class Storage {

  public ActivityStorage getActivity() {
    return activity;
  }
  public void setActivity(ActivityStorage activity) {
    this.activity = activity;
  }
  public WorkflowStorage getWorkflow() {
    return workflow;
  }
  public void setWorkflow(WorkflowStorage workflow) {
    this.workflow = workflow;
  }
  private ActivityStorage activity;
  private WorkflowStorage workflow;
}
